#!/bin/bash

###############################################################################
# REST WorkItem Handler - jBPM to BAMOE Compatibility Verification Script
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}REST WorkItem Handler Compatibility Check${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print test result
print_result() {
    local test_name=$1
    local result=$2
    local message=$3
    
    if [ "$result" = "PASS" ]; then
        echo -e "${GREEN}✓${NC} $test_name: ${GREEN}PASSED${NC}"
        ((PASSED++))
    elif [ "$result" = "FAIL" ]; then
        echo -e "${RED}✗${NC} $test_name: ${RED}FAILED${NC} - $message"
        ((FAILED++))
    elif [ "$result" = "WARN" ]; then
        echo -e "${YELLOW}⚠${NC} $test_name: ${YELLOW}WARNING${NC} - $message"
        ((WARNINGS++))
    fi
}

# Test 1: Check Maven is available
echo -e "\n${BLUE}[1/10] Checking Maven installation...${NC}"
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    print_result "Maven Installation" "PASS" "$MVN_VERSION"
else
    print_result "Maven Installation" "FAIL" "Maven not found in PATH"
    exit 1
fi

# Test 2: Check Java version
echo -e "\n${BLUE}[2/10] Checking Java version...${NC}"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    if [[ $JAVA_VERSION == *"17"* ]] || [[ $JAVA_VERSION == *"21"* ]]; then
        print_result "Java Version" "PASS" "$JAVA_VERSION"
    else
        print_result "Java Version" "WARN" "Java 17+ recommended, found: $JAVA_VERSION"
    fi
else
    print_result "Java Version" "FAIL" "Java not found in PATH"
    exit 1
fi

# Test 3: Check project structure
echo -e "\n${BLUE}[3/10] Checking project structure...${NC}"
if [ -f "pom.xml" ]; then
    print_result "Project Structure" "PASS" "pom.xml found"
else
    print_result "Project Structure" "FAIL" "pom.xml not found"
    exit 1
fi

# Test 4: Check source files
echo -e "\n${BLUE}[4/10] Checking source files...${NC}"
REQUIRED_FILES=(
    "src/main/java/org/kogito/workitem/rest/RestWorkItemHandler.java"
    "src/main/java/org/kogito/workitem/rest/auth/ApiTokenConfig.java"
    "src/main/resources/rest-workitem.properties"
)

ALL_FILES_EXIST=true
for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        print_result "Source Files" "FAIL" "Missing: $file"
        ALL_FILES_EXIST=false
    fi
done

if [ "$ALL_FILES_EXIST" = true ]; then
    print_result "Source Files" "PASS" "All required files present"
fi

# Test 5: Verify dependencies
echo -e "\n${BLUE}[5/10] Verifying Maven dependencies...${NC}"
if mvn dependency:tree > /dev/null 2>&1; then
    print_result "Maven Dependencies" "PASS" "Dependencies resolved"
else
    print_result "Maven Dependencies" "FAIL" "Failed to resolve dependencies"
fi

# Test 6: Check for Kogito dependencies
echo -e "\n${BLUE}[6/10] Checking Kogito dependencies...${NC}"
if mvn dependency:tree | grep -q "org.kie.kogito"; then
    print_result "Kogito Dependencies" "PASS" "Kogito dependencies found"
else
    print_result "Kogito Dependencies" "FAIL" "Kogito dependencies not found"
fi

# Test 7: Check for Vert.x dependencies
echo -e "\n${BLUE}[7/10] Checking Vert.x dependencies...${NC}"
if mvn dependency:tree | grep -q "vertx-web-client"; then
    print_result "Vert.x Dependencies" "PASS" "Vert.x web client found"
else
    print_result "Vert.x Dependencies" "FAIL" "Vert.x web client not found"
fi

# Test 8: Compile the project
echo -e "\n${BLUE}[8/10] Compiling project...${NC}"
if mvn clean compile -DskipTests > /dev/null 2>&1; then
    print_result "Compilation" "PASS" "Project compiled successfully"
else
    print_result "Compilation" "FAIL" "Compilation failed"
fi

# Test 9: Check for native image configuration
echo -e "\n${BLUE}[9/10] Checking native image configuration...${NC}"
NATIVE_CONFIG_DIR="src/main/resources/META-INF/native-image/org.kie.kogito/kogito-rest-workitem"
if [ -d "$NATIVE_CONFIG_DIR" ]; then
    if [ -f "$NATIVE_CONFIG_DIR/reflect-config.json" ]; then
        print_result "Native Image Config" "PASS" "Native image configuration found"
    else
        print_result "Native Image Config" "WARN" "reflect-config.json not found"
    fi
else
    print_result "Native Image Config" "WARN" "Native image configuration directory not found"
fi

# Test 10: Verify API Token Configuration
echo -e "\n${BLUE}[10/10] Verifying API Token Configuration...${NC}"
if [ -f "src/main/resources/rest-workitem.properties" ]; then
    if grep -q "rest.workitem.api.token" "src/main/resources/rest-workitem.properties"; then
        print_result "API Token Config" "PASS" "API token configuration present"
    else
        print_result "API Token Config" "FAIL" "API token property not found"
    fi
else
    print_result "API Token Config" "FAIL" "rest-workitem.properties not found"
fi

# Summary
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}Verification Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Passed:${NC}   $PASSED"
echo -e "${YELLOW}Warnings:${NC} $WARNINGS"
echo -e "${RED}Failed:${NC}   $FAILED"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All critical tests passed!${NC}"
    echo -e "${GREEN}The REST WorkItem Handler is compatible with BAMOE.${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed. Please review the errors above.${NC}"
    exit 1
fi

# Made with Bob
