# BAMOE v8 to v9 WorkItem Handler Migration Guide
## Complete Task Checklist for REST WorkItem Handler Migration

## Overview

This guide provides a comprehensive task checklist for migrating WorkItem handlers from BAMOE v8 (jBPM-based) to BAMOE v9 (Kogito-based). BAMOE v9 represents a significant architectural shift from traditional jBPM to cloud-native Kogito runtime.

## Key Architectural Changes

| Aspect | BAMOE v8 (jBPM) | BAMOE v9 (Kogito) |
|--------|-----------------|-------------------|
| Runtime | Traditional jBPM Engine | Kogito Cloud-Native Runtime |
| Deployment | WAR/EAR on Application Server | Microservices (Quarkus/Spring Boot) |
| Work Item API | `org.kie.api.runtime.process.WorkItemHandler` | `org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler` |
| Registration | Manual via `ksession.getWorkItemManager()` | ServiceLoader or CDI/Spring injection |
| Configuration | System properties or programmatic | Properties files + Environment variables |
| Native Compilation | Not supported | GraalVM native image support |

## Migration Task Checklist

### Phase 1: Pre-Migration Assessment (Planning)

#### Task 1.1: Inventory Current WorkItem Handlers
- [ ] List all custom WorkItem handlers in your BAMOE v8 project
- [ ] Document handler names and their purposes
- [ ] Identify dependencies for each handler
- [ ] Review handler registration mechanisms

**Action Items:**
```bash
# Find all WorkItemHandler implementations
find . -name "*.java" -exec grep -l "implements WorkItemHandler" {} \;

# Find handler registrations
find . -name "*.java" -exec grep -l "registerWorkItemHandler" {} \;
```

#### Task 1.2: Analyze Handler Dependencies
- [ ] List external libraries used by handlers
- [ ] Check if libraries are compatible with Quarkus/Spring Boot
- [ ] Identify deprecated APIs or libraries
- [ ] Document any custom authentication mechanisms

**Checklist:**
```
□ HTTP clients (Apache HttpClient, OkHttp, etc.)
□ JSON/XML parsers (Jackson, JAXB, etc.)
□ Authentication libraries (OAuth, JWT, etc.)
□ Database connections
□ Message queue clients
□ Custom business logic libraries
```

#### Task 1.3: Review Process Definitions
- [ ] Identify all BPMN processes using WorkItem handlers
- [ ] Document task names and parameters
- [ ] Check for custom data mappings
- [ ] Review error handling strategies

**Action Items:**
```bash
# Find BPMN files with service tasks
find . -name "*.bpmn*" -exec grep -l "serviceTask" {} \;

# Extract task names
grep -r "tns:taskName" --include="*.bpmn*" .
```

### Phase 2: Dependency Migration

#### Task 2.1: Update Maven/Gradle Dependencies
- [ ] Remove jBPM v8 dependencies
- [ ] Add Kogito/BAMOE v9 dependencies
- [ ] Update parent POM/BOM versions
- [ ] Resolve dependency conflicts

**Maven Changes:**

**REMOVE (v8):**
```xml
<dependency>
    <groupId>org.jbpm</groupId>
    <artifactId>jbpm-workitems-rest</artifactId>
    <version>7.x.x</version>
</dependency>
<dependency>
    <groupId>org.kie</groupId>
    <artifactId>kie-api</artifactId>
    <version>7.x.x</version>
</dependency>
```

**ADD (v9):**
```xml
<dependency>
    <groupId>org.kie.kogito</groupId>
    <artifactId>kogito-rest-workitem</artifactId>
    <version>9.x.x</version>
</dependency>
<dependency>
    <groupId>org.kie.kogito</groupId>
    <artifactId>kogito-api</artifactId>
    <version>9.x.x</version>
</dependency>
```

#### Task 2.2: Update HTTP Client Dependencies
- [ ] Replace Apache HttpClient with Vert.x Web Client (recommended for Kogito)
- [ ] Update to reactive/async patterns if needed
- [ ] Configure connection pooling

**Migration:**
```xml
<!-- v8: Apache HttpClient -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
</dependency>

<!-- v9: Vert.x Web Client (Reactive) -->
<dependency>
    <groupId>io.smallrye.reactive</groupId>
    <artifactId>smallrye-mutiny-vertx-web-client</artifactId>
</dependency>
```

#### Task 2.3: Update Framework Dependencies
- [ ] Choose runtime: Quarkus or Spring Boot
- [ ] Add framework-specific dependencies
- [ ] Configure build plugins

**For Quarkus:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-kogito</artifactId>
</dependency>
```

**For Spring Boot:**
```xml
<dependency>
    <groupId>org.kie.kogito</groupId>
    <artifactId>kogito-spring-boot-starter</artifactId>
</dependency>
```

### Phase 3: Code Migration

#### Task 3.1: Update Package Imports
- [ ] Replace jBPM imports with Kogito imports
- [ ] Update WorkItemHandler interface references
- [ ] Fix compilation errors

**Import Changes:**

**v8 Imports:**
```java
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
```

**v9 Imports:**
```java
import org.kie.kogito.internal.process.workitem.KogitoWorkItem;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemManager;
import org.kie.kogito.internal.process.workitem.WorkItemTransition;
```

#### Task 3.2: Refactor Handler Implementation
- [ ] Change interface from `WorkItemHandler` to `KogitoWorkItemHandler`
- [ ] Extend `DefaultKogitoWorkItemHandler` (recommended)
- [ ] Update method signatures
- [ ] Implement new lifecycle methods

**v8 Handler:**
```java
public class RestWorkItemHandler implements WorkItemHandler {
    
    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        // Execute REST call
        Map<String, Object> results = new HashMap<>();
        results.put("Result", response);
        manager.completeWorkItem(workItem.getId(), results);
    }
    
    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        // Cleanup
    }
}
```

**v9 Handler:**
```java
public class RestWorkItemHandler extends DefaultKogitoWorkItemHandler {
    
    @Override
    public Optional<WorkItemTransition> activateWorkItemHandler(
            KogitoWorkItemManager manager,
            KogitoWorkItemHandler handler,
            KogitoWorkItem workItem,
            WorkItemTransition transition) {
        
        // Execute REST call
        Map<String, Object> results = new HashMap<>();
        results.put("Result", response);
        
        return Optional.of(
            this.workItemLifeCycle.newTransition("complete",
                workItem.getPhaseStatus(),
                results)
        );
    }
}
```

#### Task 3.3: Update HTTP Client Code
- [ ] Replace synchronous HTTP calls with reactive/async
- [ ] Update request building
- [ ] Handle responses with Mutiny Uni/Multi

**v8 (Apache HttpClient):**
```java
HttpClient client = HttpClients.createDefault();
HttpGet request = new HttpGet(url);
HttpResponse response = client.execute(request);
String result = EntityUtils.toString(response.getEntity());
```

**v9 (Vert.x Web Client):**
```java
WebClient client = WebClient.create(vertx);
HttpResponse<Buffer> response = client
    .getAbs(url)
    .sendAndAwait();
String result = response.bodyAsString();
```

#### Task 3.4: Implement Configuration Management
- [ ] Create properties file for configuration
- [ ] Implement configuration loader
- [ ] Support environment variables
- [ ] Add validation

**Create Configuration:**
```properties
# rest-workitem.properties
rest.workitem.api.token=${API_TOKEN:}
rest.workitem.api.token.header=Authorization
rest.workitem.api.token.prefix=Bearer 
rest.workitem.timeout.millis=30000
rest.workitem.retry.attempts=3
```

**Configuration Loader:**
```java
public class WorkItemConfig {
    private static final Properties props = new Properties();
    
    static {
        try (InputStream input = WorkItemConfig.class
                .getClassLoader()
                .getResourceAsStream("rest-workitem.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException ex) {
            logger.error("Failed to load configuration", ex);
        }
    }
    
    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, 
            System.getenv(key.toUpperCase().replace(".", "_")));
    }
}
```

### Phase 4: Handler Registration

#### Task 4.1: Remove Manual Registration
- [ ] Remove `ksession.getWorkItemManager().registerWorkItemHandler()` calls
- [ ] Remove handler factory classes (if any)
- [ ] Clean up initialization code

#### Task 4.2: Implement ServiceLoader Registration
- [ ] Create `META-INF/services` directory
- [ ] Add service provider configuration file
- [ ] List handler implementations

**Create File:**
```
src/main/resources/META-INF/services/org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler
```

**Content:**
```
org.kogito.workitem.rest.RestWorkItemHandler
```

#### Task 4.3: Configure CDI/Spring Injection (Alternative)
- [ ] Add framework annotations
- [ ] Configure bean scope
- [ ] Implement producer methods if needed

**Quarkus CDI:**
```java
@ApplicationScoped
public class RestWorkItemHandler extends DefaultKogitoWorkItemHandler {
    // Implementation
}
```

**Spring:**
```java
@Component
public class RestWorkItemHandler extends DefaultKogitoWorkItemHandler {
    // Implementation
}
```

### Phase 5: Authentication Migration

#### Task 5.1: Migrate Authentication Mechanisms
- [ ] Review current authentication (Basic, OAuth, API Key)
- [ ] Implement new authentication decorators
- [ ] Configure authentication in properties
- [ ] Test authentication flow

**v8 Authentication:**
```java
// Hardcoded or passed as parameters
request.setHeader("Authorization", "Bearer " + token);
```

**v9 Authentication:**
```properties
# Properties-based configuration
rest.workitem.api.token=your-token-here
rest.workitem.api.token.header=Authorization
rest.workitem.api.token.prefix=Bearer 
```

```java
// Automatic application via ApiTokenConfig
private void applyAuthentication(HttpRequest<Buffer> request) {
    if (apiTokenConfig.isTokenConfigured()) {
        request.putHeader(
            apiTokenConfig.getTokenHeader(),
            apiTokenConfig.getAuthorizationValue()
        );
    }
}
```

#### Task 5.2: Implement OAuth2 Support (if needed)
- [ ] Add OAuth2 dependencies
- [ ] Configure OAuth2 client
- [ ] Implement token refresh logic
- [ ] Handle token expiration

**OAuth2 Configuration:**
```properties
rest.workitem.oauth2.client.id=${OAUTH2_CLIENT_ID}
rest.workitem.oauth2.client.secret=${OAUTH2_CLIENT_SECRET}
rest.workitem.oauth2.token.url=https://auth.example.com/token
rest.workitem.oauth2.scope=api.read api.write
```

### Phase 6: Process Definition Updates

#### Task 6.1: Update BPMN Task Definitions
- [ ] Verify task names match handler names
- [ ] Update data input/output mappings
- [ ] Review parameter names
- [ ] Test process execution

**v8 BPMN:**
```xml
<serviceTask id="restTask" name="REST Call" 
             implementation="##WebService"
             operationRef="_restOperation">
    <ioSpecification>
        <dataInput id="Url" name="Url"/>
        <dataInput id="Method" name="Method"/>
        <dataOutput id="Result" name="Result"/>
    </ioSpecification>
</serviceTask>
```

**v9 BPMN (Same structure, but verify):**
```xml
<serviceTask id="restTask" name="REST Call" 
             tns:taskName="Rest">
    <ioSpecification>
        <dataInput id="Url" name="Url"/>
        <dataInput id="Method" name="Method"/>
        <dataOutput id="Result" name="Result"/>
    </ioSpecification>
</serviceTask>
```

#### Task 6.2: Update Process Variables
- [ ] Review variable types
- [ ] Update custom types if needed
- [ ] Verify serialization/deserialization
- [ ] Test with complex objects

### Phase 7: Native Image Support

#### Task 7.1: Add Native Image Configuration
- [ ] Create reflection configuration
- [ ] Add resource configuration
- [ ] Configure native-image properties
- [ ] Test native build

**Create Directory Structure:**
```
src/main/resources/META-INF/native-image/
    org.kie.kogito/
        kogito-rest-workitem/
            reflect-config.json
            resource-config.json
            native-image.properties
```

**reflect-config.json:**
```json
[
  {
    "name": "org.kogito.workitem.rest.RestWorkItemHandler",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "org.kogito.workitem.rest.auth.ApiTokenConfig",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true
  }
]
```

**resource-config.json:**
```json
{
  "resources": {
    "includes": [
      {
        "pattern": "rest-workitem\\.properties"
      }
    ]
  }
}
```

#### Task 7.2: Test Native Compilation
- [ ] Build native image
- [ ] Test handler functionality
- [ ] Verify performance
- [ ] Check memory usage

**Build Commands:**
```bash
# Quarkus native build
mvn clean package -Pnative

# Test native executable
./target/*-runner
```

### Phase 8: Testing

#### Task 8.1: Unit Testing
- [ ] Create unit tests for handler
- [ ] Mock external dependencies
- [ ] Test error scenarios
- [ ] Verify parameter handling

**Test Template:**
```java
@QuarkusTest
public class RestWorkItemHandlerTest {
    
    @Inject
    RestWorkItemHandler handler;
    
    @Test
    public void testHandlerExecution() {
        // Setup
        Map<String, Object> params = new HashMap<>();
        params.put("Url", "https://api.example.com/test");
        params.put("Method", "GET");
        
        KogitoWorkItem workItem = createMockWorkItem(params);
        
        // Execute
        Optional<WorkItemTransition> result = 
            handler.activateWorkItemHandler(manager, handler, workItem, transition);
        
        // Verify
        assertTrue(result.isPresent());
        assertNotNull(result.get().data().get("Result"));
    }
}
```

#### Task 8.2: Integration Testing
- [ ] Test with actual BPMN processes
- [ ] Verify end-to-end flow
- [ ] Test with real external APIs (or mocks)
- [ ] Validate error handling

**Integration Test:**
```java
@QuarkusTest
public class RestProcessIntegrationTest {
    
    @Inject
    Process<RestProcessModel> restProcess;
    
    @Test
    public void testRestProcessExecution() {
        RestProcessModel model = new RestProcessModel();
        model.setApiUrl("https://api.example.com/data");
        
        ProcessInstance<RestProcessModel> instance = 
            restProcess.createInstance(model);
        instance.start();
        
        assertEquals(ProcessInstance.STATE_COMPLETED, instance.status());
        assertNotNull(instance.variables().getResult());
    }
}
```

#### Task 8.3: Performance Testing
- [ ] Benchmark handler execution time
- [ ] Test concurrent executions
- [ ] Monitor memory usage
- [ ] Compare with v8 performance

**Performance Test:**
```java
@Test
public void testPerformance() {
    int iterations = 1000;
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < iterations; i++) {
        handler.activateWorkItemHandler(manager, handler, workItem, transition);
    }
    
    long duration = System.currentTimeMillis() - startTime;
    double avgTime = duration / (double) iterations;
    
    logger.info("Average execution time: {} ms", avgTime);
    assertTrue(avgTime < 100, "Handler should execute in under 100ms");
}
```

### Phase 9: Configuration & Deployment

#### Task 9.1: Environment Configuration
- [ ] Set up development environment
- [ ] Configure staging environment
- [ ] Prepare production configuration
- [ ] Document environment variables

**Environment Variables:**
```bash
# Development
export API_TOKEN=dev-token-123
export REST_WORKITEM_TIMEOUT_MILLIS=30000

# Staging
export API_TOKEN=staging-token-456
export REST_WORKITEM_TIMEOUT_MILLIS=60000

# Production
export API_TOKEN=${VAULT_API_TOKEN}
export REST_WORKITEM_TIMEOUT_MILLIS=120000
export REST_WORKITEM_RETRY_ATTEMPTS=5
```

#### Task 9.2: Container Configuration
- [ ] Create Dockerfile
- [ ] Configure container resources
- [ ] Set up health checks
- [ ] Configure logging

**Dockerfile (Quarkus):**
```dockerfile
FROM registry.access.redhat.com/ubi8/openjdk-17:latest

COPY target/quarkus-app/lib/ /deployments/lib/
COPY target/quarkus-app/*.jar /deployments/
COPY target/quarkus-app/app/ /deployments/app/
COPY target/quarkus-app/quarkus/ /deployments/quarkus/

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

EXPOSE 8080
```

#### Task 9.3: Kubernetes/OpenShift Deployment
- [ ] Create deployment manifests
- [ ] Configure ConfigMaps for properties
- [ ] Set up Secrets for sensitive data
- [ ] Configure service and routes

**ConfigMap:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rest-workitem-config
data:
  rest-workitem.properties: |
    rest.workitem.api.token.header=Authorization
    rest.workitem.api.token.prefix=Bearer 
    rest.workitem.timeout.millis=60000
```

**Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: rest-workitem-secrets
type: Opaque
stringData:
  api-token: your-secret-token-here
```

### Phase 10: Monitoring & Observability

#### Task 10.1: Add Logging
- [ ] Configure logging framework
- [ ] Add structured logging
- [ ] Set appropriate log levels
- [ ] Log important events

**Logging Configuration:**
```properties
# application.properties
quarkus.log.level=INFO
quarkus.log.category."org.kogito.workitem.rest".level=DEBUG
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
```

**Structured Logging:**
```java
logger.info("Executing REST call: method={}, url={}, timeout={}",
    method, url, timeout);
```

#### Task 10.2: Add Metrics
- [ ] Enable Micrometer/Prometheus metrics
- [ ] Add custom metrics for handler
- [ ] Configure metric endpoints
- [ ] Set up dashboards

**Metrics Configuration:**
```java
@Inject
MeterRegistry registry;

public void recordExecution(long duration, boolean success) {
    registry.timer("rest.workitem.execution",
        "success", String.valueOf(success))
        .record(duration, TimeUnit.MILLISECONDS);
}
```

#### Task 10.3: Add Health Checks
- [ ] Implement liveness probe
- [ ] Implement readiness probe
- [ ] Add custom health checks
- [ ] Test health endpoints

**Health Check:**
```java
@Liveness
@ApplicationScoped
public class RestWorkItemHealthCheck implements HealthCheck {
    
    @Override
    public HealthCheckResponse call() {
        boolean isHealthy = checkExternalApiAvailability();
        
        return HealthCheckResponse
            .named("rest-workitem-handler")
            .status(isHealthy)
            .build();
    }
}
```

### Phase 11: Documentation

#### Task 11.1: Update Technical Documentation
- [ ] Document API changes
- [ ] Update configuration guide
- [ ] Document deployment procedures
- [ ] Create troubleshooting guide

#### Task 11.2: Update User Documentation
- [ ] Update process modeling guide
- [ ] Document new features
- [ ] Provide migration examples
- [ ] Create FAQ

#### Task 11.3: Create Runbooks
- [ ] Deployment runbook
- [ ] Rollback procedures
- [ ] Incident response guide
- [ ] Monitoring guide

### Phase 12: Validation & Go-Live

#### Task 12.1: Pre-Production Validation
- [ ] Run full test suite
- [ ] Perform load testing
- [ ] Validate in staging environment
- [ ] Get stakeholder approval

**Validation Checklist:**
```
□ All unit tests pass
□ All integration tests pass
□ Performance meets SLA requirements
□ Security scan completed
□ Documentation reviewed
□ Rollback plan tested
□ Monitoring configured
□ Alerts configured
```

#### Task 12.2: Production Deployment
- [ ] Schedule deployment window
- [ ] Execute deployment plan
- [ ] Verify deployment success
- [ ] Monitor for issues

**Deployment Steps:**
```bash
# 1. Backup current version
kubectl get deployment rest-workitem -o yaml > backup.yaml

# 2. Deploy new version
kubectl apply -f deployment-v9.yaml

# 3. Monitor rollout
kubectl rollout status deployment/rest-workitem

# 4. Verify health
curl http://rest-workitem:8080/q/health

# 5. Monitor logs
kubectl logs -f deployment/rest-workitem
```

#### Task 12.3: Post-Deployment Validation
- [ ] Verify all processes execute correctly
- [ ] Check metrics and logs
- [ ] Validate performance
- [ ] Confirm no errors

**Validation Commands:**
```bash
# Check pod status
kubectl get pods -l app=rest-workitem

# Check logs for errors
kubectl logs -l app=rest-workitem | grep ERROR

# Check metrics
curl http://rest-workitem:8080/q/metrics

# Test REST endpoint
curl -X POST http://rest-workitem:8080/rest-process \
  -H "Content-Type: application/json" \
  -d '{"apiUrl": "https://api.example.com/test"}'
```

## Common Migration Issues & Solutions

### Issue 1: ClassNotFoundException
**Symptom:** Handler class not found at runtime

**Solution:**
- Verify ServiceLoader configuration
- Check META-INF/services file
- Ensure handler is in classpath

### Issue 2: Authentication Failures
**Symptom:** 401/403 errors from external APIs

**Solution:**
- Verify API token configuration
- Check token format and prefix
- Enable DEBUG logging
- Test token manually with curl

### Issue 3: Native Image Build Failures
**Symptom:** Reflection errors in native build

**Solution:**
- Add missing classes to reflect-config.json
- Use `--initialize-at-build-time` for static initialization
- Check resource-config.json for missing resources

### Issue 4: Performance Degradation
**Symptom:** Slower execution than v8

**Solution:**
- Review async/reactive patterns
- Check connection pooling configuration
- Optimize HTTP client settings
- Profile application

### Issue 5: Process Execution Failures
**Symptom:** Processes fail to complete

**Solution:**
- Verify task names match handler names
- Check parameter mappings
- Review error logs
- Test handler in isolation

## Migration Timeline Estimate

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Pre-Migration Assessment | 1-2 weeks | None |
| Dependency Migration | 1 week | Phase 1 |
| Code Migration | 2-3 weeks | Phase 2 |
| Handler Registration | 1 week | Phase 3 |
| Authentication Migration | 1-2 weeks | Phase 3 |
| Process Definition Updates | 1 week | Phase 3 |
| Native Image Support | 1 week | Phase 3 |
| Testing | 2-3 weeks | Phase 3-7 |
| Configuration & Deployment | 1-2 weeks | Phase 8 |
| Monitoring & Observability | 1 week | Phase 9 |
| Documentation | 1 week | All phases |
| Validation & Go-Live | 1 week | All phases |
| **Total** | **14-20 weeks** | |

## Success Criteria

- [ ] All WorkItem handlers migrated and functional
- [ ] All processes execute successfully
- [ ] Performance meets or exceeds v8 baseline
- [ ] Zero critical bugs in production
- [ ] Documentation complete and accurate
- [ ] Team trained on v9 architecture
- [ ] Monitoring and alerting operational
- [ ] Rollback plan tested and ready

## Conclusion

Migrating from BAMOE v8 to v9 requires careful planning and execution. This checklist provides a comprehensive guide to ensure a successful migration. Follow each phase systematically, validate thoroughly, and maintain clear communication with stakeholders throughout the process.

For additional support, refer to:
- JBPM-BAMOE-COMPATIBILITY.md
- API-TOKEN-AUTHENTICATION.md
- Official BAMOE v9 documentation