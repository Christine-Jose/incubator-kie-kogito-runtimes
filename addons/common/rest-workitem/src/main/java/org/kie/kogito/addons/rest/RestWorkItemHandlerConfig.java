package org.kie.kogito.addons.rest;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Configuration for REST WorkItem Handler authentication.
 * Supports token injection from environment variables or properties.
 *
 * Configuration priority:
 * 1. System Properties (-D flags)
 * 2. Environment Variables
 * 3. application.properties
 * 4. Default values
 */
@ApplicationScoped
public class RestWorkItemHandlerConfig {

    /**
     * API token for authentication.
     * Can be set via REST_API_TOKEN environment variable.
     */
    @ConfigProperty(name = "kogito.rest.api.token")
    Optional<String> apiToken;

    /**
     * Bearer token for OAuth2/JWT authentication.
     * Can be set via REST_API_BEARER_TOKEN environment variable.
     * Takes precedence over apiToken for Authorization header.
     */
    @ConfigProperty(name = "kogito.rest.api.bearer.token")
    Optional<String> bearerToken;

    /**
     * Custom authorization header value.
     * Can be set via REST_API_AUTHORIZATION environment variable.
     * Takes precedence over both apiToken and bearerToken.
     */
    @ConfigProperty(name = "kogito.rest.api.authorization")
    Optional<String> authorizationHeader;

    /**
     * Enable/disable automatic token injection.
     * Default: true
     */
    @ConfigProperty(name = "kogito.rest.api.token.auto-inject", defaultValue = "true")
    boolean autoInjectToken;

    /**
     * Returns the Authorization header value to use.
     * Priority: authorizationHeader > bearerToken > apiToken
     */
    public Optional<String> getAuthorizationHeader() {
        if (authorizationHeader.isPresent()) {
            return authorizationHeader;
        }

        if (bearerToken.isPresent()) {
            return Optional.of("Bearer " + bearerToken.get());
        }

        return apiToken;
    }

    public boolean isAutoInjectEnabled() {
        return autoInjectToken;
    }

    public Optional<String> getApiToken() {
        return apiToken;
    }

    public Optional<String> getBearerToken() {
        return bearerToken;
    }
}