# Quarkus Native Build Support for kogito-rest-workitem

This document explains how kogito-rest-workitem supports Quarkus native builds without reflection issues.

## Overview

The kogito-rest-workitem module uses reflection in two main areas:
1. **ServiceLoader** - To dynamically load `RequestDecorator` implementations
2. **Dynamic class loading** - To instantiate handler classes by their fully qualified names

## Native Build Configuration

To support Quarkus native builds, we've added GraalVM native-image configuration files that are automatically picked up by both GraalVM native-image and Quarkus.

### Configuration Files

All configuration files are located in:
```
src/main/resources/META-INF/native-image/org.kie.kogito/kogito-rest-workitem/
```

#### 1. reflect-config.json
Registers all classes that need reflection support:
- Auth decorators (ApiKeyAuthDecorator, BasicAuthDecorator, BearerTokenAuthDecorator, etc.)
- Body builders (DefaultWorkItemHandlerBodyBuilder)
- Params decorators (PrefixParamsDecorator, CollectionParamsDecorator)
- Path resolvers (DefaultPathParamResolver)
- Result handlers (DefaultRestWorkItemHandlerResult)
- Request decorators (HeaderMetadataDecorator)

#### 2. resource-config.json
Ensures ServiceLoader configuration files are included in the native image:
- Includes all files matching pattern `META-INF/services/.*`

#### 3. native-image.properties
Links the configuration files together and ensures they're loaded by the native-image builder.

## How It Works

### ServiceLoader Support
The `RestWorkItemHandler` uses ServiceLoader to load `RequestDecorator` implementations:
```java
this.requestDecorators = StreamSupport.stream(
    ServiceLoader.load(RequestDecorator.class).spliterator(), false)
    .collect(Collectors.toList());
```

The `resource-config.json` ensures that the service provider configuration file (`META-INF/services/org.kogito.workitem.rest.decorators.RequestDecorator`) is included in the native image.

### Dynamic Class Loading Support
The `RestWorkItemHandlerUtils.loadClass()` method dynamically instantiates classes:
```java
Thread.currentThread().getContextClassLoader()
    .loadClass(className)
    .asSubclass(clazz)
    .getConstructor()
    .newInstance();
```

The `reflect-config.json` registers all commonly used classes with:
- `allDeclaredConstructors: true` - Allows instantiation via reflection
- `allPublicConstructors: true` - Allows public constructor access
- `allDeclaredMethods: true` - Allows method invocation
- `allPublicMethods: true` - Allows public method access

## Usage in Quarkus Applications

When using kogito-rest-workitem in a Quarkus application, the native build configuration is automatically applied. No additional configuration is needed.

### Building a Native Image

```bash
mvn clean package -Pnative
```

Or with Quarkus:
```bash
./mvnw package -Pnative
```

## Adding Custom Implementations

If you create custom implementations of the following interfaces, you need to register them for reflection:

### Option 1: Using Quarkus Annotations (Recommended for Quarkus apps)
Add `@RegisterForReflection` to your custom class:
```java
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MyCustomAuthDecorator implements AuthDecorator {
    // implementation
}
```

### Option 2: Using application.properties (Quarkus)
Add to your `application.properties`:
```properties
quarkus.native.additional-build-args=\
    -H:ReflectionConfigurationFiles=my-reflect-config.json
```

### Option 3: Using reflect-config.json (GraalVM native-image)
Create or extend `reflect-config.json` in your application:
```json
[
  {
    "name": "com.example.MyCustomAuthDecorator",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  }
]
```

## Supported Classes

The following classes are pre-registered for reflection:

### Auth Decorators
- `org.kogito.workitem.rest.auth.ApiKeyAuthDecorator`
- `org.kogito.workitem.rest.auth.BasicAuthDecorator`
- `org.kogito.workitem.rest.auth.BearerTokenAuthDecorator`
- `org.kogito.workitem.rest.auth.ClientOAuth2AuthDecorator`
- `org.kogito.workitem.rest.auth.PasswordOAuth2AuthDecorator`

### Body Builders
- `org.kogito.workitem.rest.bodybuilders.DefaultWorkItemHandlerBodyBuilder`

### Params Decorators
- `org.kogito.workitem.rest.decorators.PrefixParamsDecorator`
- `org.kogito.workitem.rest.decorators.CollectionParamsDecorator`

### Path Resolvers
- `org.kogito.workitem.rest.pathresolvers.DefaultPathParamResolver`

### Result Handlers
- `org.kogito.workitem.rest.resulthandlers.DefaultRestWorkItemHandlerResult`

### Request Decorators
- `org.kogito.workitem.rest.decorators.HeaderMetadataDecorator`

## Troubleshooting

### ClassNotFoundException in Native Image
If you encounter `ClassNotFoundException` for a custom class:
1. Ensure the class is registered for reflection (see "Adding Custom Implementations")
2. Verify the class is on the classpath
3. Check that the class has a no-arg constructor

### ServiceLoader Not Finding Implementations
If ServiceLoader doesn't find your custom implementation:
1. Ensure you have a proper service provider configuration file in `META-INF/services/`
2. Verify the resource-config.json includes the service file pattern
3. Check that the implementation class is registered for reflection

### Method Invocation Errors
If you get errors invoking methods on reflected classes:
1. Ensure `allDeclaredMethods: true` is set in reflect-config.json
2. For public methods only, use `allPublicMethods: true`
3. Consider registering specific methods if you want fine-grained control

## Testing Native Builds

To test that your native build works correctly:

1. Build the native image:
   ```bash
   mvn clean package -Pnative
   ```

2. Run the native executable:
   ```bash
   ./target/*-runner
   ```

3. Test REST work item functionality to ensure all reflection-based features work

## References

- [GraalVM Native Image Configuration](https://www.graalvm.org/latest/reference-manual/native-image/metadata/)
- [Quarkus Native Build Guide](https://quarkus.io/guides/building-native-image)
- [ServiceLoader in Native Image](https://www.graalvm.org/latest/reference-manual/native-image/dynamic-features/Resources/)