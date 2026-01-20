# jBPM to BAMOE Compatibility Verification Guide
## REST WorkItem Handler Migration

## Overview

This guide provides a comprehensive approach to verify the compatibility of the REST WorkItem Handler when migrating from jBPM to BAMOE (Business Automation Manager Open Edition). The REST WorkItem Handler with API token authentication is designed to be compatible with both platforms.

## Compatibility Matrix

| Component | jBPM Version | BAMOE Version | Status |
|-----------|--------------|---------------|---------|
| REST WorkItem Handler | 7.x+ | 9.x+ | ✅ Compatible |
| API Token Authentication | New Feature | New Feature | ✅ Compatible |
| Kogito Runtime | 1.x+ | 9.x+ | ✅ Compatible |
| Vert.x Web Client | 4.x+ | 4.x+ | ✅ Compatible |

## Architecture Compatibility

### 1. **Core Dependencies**

The REST WorkItem Handler uses standard Kogito APIs that are compatible with both jBPM and BAMOE:

```xml
<dependency>
    <groupId>org.kie.kogito</groupId>
    <artifactId>jbpm-deps-group-engine</artifactId>
    <type>pom</type>
</dependency>
```

**Verification Steps:**
- ✅ Uses `org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler` interface
- ✅ Extends `org.kie.kogito.process.workitems.impl.DefaultKogitoWorkItemHandler`
- ✅ No platform-specific dependencies

### 2. **API Compatibility**

The handler implements standard Kogito interfaces:

```java
public class RestWorkItemHandler extends DefaultKogitoWorkItemHandler
```

**Key Interfaces:**
- `KogitoWorkItemHandler` - Core handler interface
- `WorkItemTransition` - State transition handling
- `KogitoWorkItem` - Work item abstraction

## Verification Checklist

### Phase 1: Pre-Migration Assessment

- [ ] **Review Current jBPM Version**
  ```bash
  # Check jBPM version in your project
  mvn dependency:tree | grep jbpm
  ```

- [ ] **Identify REST WorkItem Usage**
  ```bash
  # Search for REST task definitions in BPMN files
  find . -name "*.bpmn*" -exec grep -l "Rest" {} \;
  ```

- [ ] **Document Current Authentication Methods**
  - Basic Authentication
  - OAuth2
  - API Keys
  - Custom headers

### Phase 2: Dependency Verification

#### Step 1: Check Maven Dependencies

```bash
# Verify kogito-rest-workitem is in your dependencies
mvn dependency:tree | grep kogito-rest-workitem
```

Expected output:
```
[INFO] +- org.kie.kogito:kogito-rest-workitem:jar:999-SNAPSHOT:compile
```

#### Step 2: Verify Transitive Dependencies

```bash
# Check for Vert.x web client
mvn dependency:tree | grep vertx-web-client
```

Expected output:
```
[INFO] |  +- io.smallrye.reactive:smallrye-mutiny-vertx-web-client:jar:x.x.x:compile
```

### Phase 3: Code Compatibility Testing

#### Test 1: Handler Instantiation

Create a test class to verify handler creation:

```java
import org.kogito.workitem.rest.RestWorkItemHandler;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.core.Vertx;

public class RestWorkItemHandlerCompatibilityTest {
    
    @Test
    public void testHandlerCreation() {
        Vertx vertx = Vertx.vertx();
        WebClient httpClient = WebClient.create(vertx);
        WebClient httpsClient = WebClient.create(vertx);
        
        RestWorkItemHandler handler = new RestWorkItemHandler(httpClient, httpsClient);
        assertNotNull(handler);
    }
}
```

#### Test 2: API Token Configuration

Verify properties file loading:

```java
import org.kogito.workitem.rest.auth.ApiTokenConfig;

@Test
public void testApiTokenConfig() {
    ApiTokenConfig config = ApiTokenConfig.getInstance();
    assertNotNull(config);
    
    // Test with configured token
    if (config.isTokenConfigured()) {
        assertNotNull(config.getApiToken());
        assertNotNull(config.getTokenHeader());
        assertNotNull(config.getAuthorizationValue());
    }
}
```

#### Test 3: REST Endpoint Invocation

Test actual REST call with authentication:

```java
@Test
public void testRestCallWithAuthentication() {
    // Setup work item with REST parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("Url", "https://api.example.com/test");
    parameters.put("Method", "GET");
    
    KogitoWorkItem workItem = createMockWorkItem(parameters);
    
    // Execute handler
    Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
        manager, handler, workItem, transition
    );
    
    assertTrue(result.isPresent());
}
```

### Phase 4: BAMOE-Specific Verification

#### Step 1: BAMOE Runtime Compatibility

```bash
# Build with BAMOE BOM
mvn clean install -Dbamoe.version=9.x.x
```

#### Step 2: Verify Service Loader Registration

Check that the handler is properly registered:

```bash
# Verify META-INF/services files
find . -path "*/META-INF/services/*" -name "*WorkItemHandler*"
```

#### Step 3: Native Image Compatibility (GraalVM)

For BAMOE native builds, verify reflection configuration:

```bash
# Check native-image configuration
cat src/main/resources/META-INF/native-image/org.kie.kogito/kogito-rest-workitem/reflect-config.json
```

### Phase 5: Integration Testing

#### Test Scenario 1: Basic REST Call

```xml
<!-- BPMN Process Definition -->
<serviceTask id="restTask" name="Call REST API" implementation="##WebService">
  <ioSpecification>
    <dataInput id="Url" name="Url"/>
    <dataInput id="Method" name="Method"/>
    <dataOutput id="Result" name="Result"/>
  </ioSpecification>
  <dataInputAssociation>
    <targetRef>Url</targetRef>
    <assignment>
      <from>https://api.example.com/data</from>
      <to>Url</to>
    </assignment>
  </dataInputAssociation>
</serviceTask>
```

#### Test Scenario 2: Authenticated REST Call

Configure `rest-workitem.properties`:
```properties
rest.workitem.api.token=test-token-123
rest.workitem.api.token.header=Authorization
rest.workitem.api.token.prefix=Bearer 
```

Execute process and verify:
- Token is added to request headers
- Authentication succeeds
- Response is properly handled

### Phase 6: Performance Verification

#### Benchmark Tests

```java
@Test
public void testPerformance() {
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < 1000; i++) {
        // Execute REST work item
        handler.activateWorkItemHandler(manager, handler, workItem, transition);
    }
    
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    
    // Verify performance is acceptable
    assertTrue(duration < 10000, "1000 calls should complete in under 10 seconds");
}
```

## Migration Steps

### 1. Update Dependencies

**From jBPM:**
```xml
<dependency>
    <groupId>org.jbpm</groupId>
    <artifactId>jbpm-workitems-rest</artifactId>
    <version>7.x.x</version>
</dependency>
```

**To BAMOE:**
```xml
<dependency>
    <groupId>org.kie.kogito</groupId>
    <artifactId>kogito-rest-workitem</artifactId>
    <version>9.x.x</version>
</dependency>
```

### 2. Update Configuration

Add API token configuration:
```properties
# rest-workitem.properties
rest.workitem.api.token=${API_TOKEN}
rest.workitem.api.token.header=Authorization
rest.workitem.api.token.prefix=Bearer 
```

### 3. Update Process Definitions

Ensure BPMN files use correct task type:
```xml
<task id="restTask" name="REST Call" tns:taskName="Rest">
```

### 4. Update Handler Registration

**jBPM (Old):**
```java
ksession.getWorkItemManager().registerWorkItemHandler("Rest", new RestWorkItemHandler());
```

**BAMOE (New):**
```java
// Handler is auto-registered via ServiceLoader
// Or manually if needed:
workItemHandlerConfig.register("Rest", restWorkItemHandler);
```

## Troubleshooting

### Issue 1: Handler Not Found

**Symptom:** `WorkItemHandler for 'Rest' not found`

**Solution:**
1. Verify dependency is in classpath
2. Check ServiceLoader registration
3. Ensure correct task name in BPMN

### Issue 2: Authentication Fails

**Symptom:** 401 Unauthorized responses

**Solution:**
1. Verify `rest-workitem.properties` is in classpath
2. Check token value is correct
3. Enable DEBUG logging:
   ```properties
   logging.level.org.kogito.workitem.rest=DEBUG
   ```

### Issue 3: Native Image Build Fails

**Symptom:** Reflection errors in native build

**Solution:**
1. Verify `reflect-config.json` is present
2. Add missing classes to reflection configuration
3. Use `--initialize-at-build-time` for static initialization

## Validation Checklist

- [ ] All dependencies resolved successfully
- [ ] Handler instantiates without errors
- [ ] API token configuration loads correctly
- [ ] REST calls execute successfully
- [ ] Authentication headers are applied
- [ ] Response handling works as expected
- [ ] Performance meets requirements
- [ ] Native image builds successfully (if applicable)
- [ ] Integration tests pass
- [ ] No regression in existing functionality

## Best Practices

1. **Version Alignment**: Ensure all Kogito/BAMOE dependencies use the same version
2. **Testing**: Create comprehensive integration tests before migration
3. **Gradual Migration**: Migrate one process at a time
4. **Monitoring**: Add logging to track REST calls and authentication
5. **Documentation**: Document any custom configurations or workarounds

## Support Resources

- **BAMOE Documentation**: https://access.redhat.com/documentation/en-us/red_hat_build_of_kogito
- **Kogito Community**: https://kogito.kie.org/
- **GitHub Issues**: https://github.com/apache/incubator-kie-kogito-runtimes/issues

## Conclusion

The REST WorkItem Handler with API token authentication is fully compatible with both jBPM and BAMOE. By following this verification guide, you can ensure a smooth migration with minimal disruption to your business processes.

For additional support or questions, please refer to the API-TOKEN-AUTHENTICATION.md documentation or contact the BAMOE support team.