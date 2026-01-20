# API Token Authentication for REST Work Item Handler

## Overview

The REST Work Item Handler now supports API token authentication through a properties file configuration. This feature allows you to securely authenticate REST API calls by reading an API token from a properties file before executing the work item handler.

## Configuration

### 1. Properties File Setup

Create or edit the `rest-workitem.properties` file in your classpath (typically in `src/main/resources/`):

```properties
# REST Work Item Handler Configuration
# API Token for authentication
rest.workitem.api.token=your-api-token-here

# Optional: Token header name (default: Authorization)
rest.workitem.api.token.header=Authorization

# Optional: Token prefix (e.g., "Bearer ", "Token ", etc.)
rest.workitem.api.token.prefix=Bearer 
```

### 2. Configuration Properties

| Property | Description | Default Value | Required |
|----------|-------------|---------------|----------|
| `rest.workitem.api.token` | The API token value | - | Yes |
| `rest.workitem.api.token.header` | HTTP header name for the token | `Authorization` | No |
| `rest.workitem.api.token.prefix` | Prefix to add before the token | `Bearer ` | No |

## How It Works

1. **Initialization**: When the `RestWorkItemHandler` is instantiated, it loads the `ApiTokenConfig` singleton which reads the properties file.

2. **Authentication**: Before executing any REST request, the handler checks if an API token is configured:
   - If configured, it adds the token to the request headers
   - If not configured, the request proceeds without token authentication

3. **Header Format**: The authentication header is constructed as:
   ```
   <header-name>: <prefix><token>
   ```
   
   For example, with default settings:
   ```
   Authorization: Bearer your-api-token-here
   ```

## Usage Examples

### Example 1: Bearer Token Authentication

```properties
rest.workitem.api.token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
rest.workitem.api.token.header=Authorization
rest.workitem.api.token.prefix=Bearer 
```

This will add the header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Example 2: Custom API Key Authentication

```properties
rest.workitem.api.token=abc123def456
rest.workitem.api.token.header=X-API-Key
rest.workitem.api.token.prefix=
```

This will add the header:
```
X-API-Key: abc123def456
```

### Example 3: Token Authentication

```properties
rest.workitem.api.token=my-secret-token
rest.workitem.api.token.header=Authorization
rest.workitem.api.token.prefix=Token 
```

This will add the header:
```
Authorization: Token my-secret-token
```

## Security Considerations

1. **Never commit tokens to version control**: Use environment variables or secure configuration management
2. **Use external configuration**: Consider using Spring Cloud Config, Kubernetes Secrets, or similar solutions
3. **Rotate tokens regularly**: Implement a token rotation strategy
4. **Use HTTPS**: Always use HTTPS endpoints when transmitting tokens

## Environment-Specific Configuration

You can override properties using system properties or environment variables:

```bash
# Using system property
java -Drest.workitem.api.token=your-token-here ...

# Using environment variable (if supported by your framework)
export REST_WORKITEM_API_TOKEN=your-token-here
```

## Logging

The implementation includes logging at different levels:

- **INFO**: When API token authentication is successfully applied
- **DEBUG**: Detailed information about authentication process
- **WARN**: When properties file is not found

To enable debug logging, configure your logging framework:

```properties
# For SLF4J/Logback
logging.level.org.kogito.workitem.rest.auth=DEBUG
```

## Compatibility

This feature is backward compatible. If no API token is configured in the properties file, the REST Work Item Handler will function as before, using other authentication methods (Basic Auth, OAuth2, etc.) if configured through work item parameters.

## Implementation Details

### Key Classes

1. **`ApiTokenConfig`**: Singleton class that loads and provides access to token configuration
2. **`RestWorkItemHandler`**: Modified to apply token authentication before request execution

### Authentication Flow

```
RestWorkItemHandler.activateWorkItemHandler()
    ↓
Create HTTP Request
    ↓
applyApiTokenAuthentication() ← Reads from ApiTokenConfig
    ↓
Apply other decorators (RequestDecorators, AuthDecorators, etc.)
    ↓
Execute Request
```

## Troubleshooting

### Token Not Being Applied

1. Verify the properties file is in the classpath
2. Check the property name is exactly `rest.workitem.api.token`
3. Ensure the token value is not empty
4. Enable DEBUG logging to see authentication details

### Properties File Not Found

If you see a warning about the properties file not being found:
- Ensure `rest-workitem.properties` is in `src/main/resources/`
- Verify the file is included in your build output
- Check the classpath configuration

## Migration Guide

If you're upgrading from a previous version:

1. Create the `rest-workitem.properties` file
2. Add your API token configuration
3. No code changes required - the feature is automatically enabled
4. Existing authentication methods continue to work alongside token authentication