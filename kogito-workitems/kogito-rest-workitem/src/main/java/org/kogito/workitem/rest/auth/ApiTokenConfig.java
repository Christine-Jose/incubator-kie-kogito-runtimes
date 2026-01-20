/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kogito.workitem.rest.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for reading API token from properties file.
 * This class loads the rest-workitem.properties file and provides
 * access to authentication configuration.
 */
public class ApiTokenConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApiTokenConfig.class);
    private static final String PROPERTIES_FILE = "rest-workitem.properties";
    private static final String API_TOKEN_KEY = "rest.workitem.api.token";
    private static final String TOKEN_HEADER_KEY = "rest.workitem.api.token.header";
    private static final String TOKEN_PREFIX_KEY = "rest.workitem.api.token.prefix";

    private static final String DEFAULT_HEADER = "Authorization";
    private static final String DEFAULT_PREFIX = "Bearer ";

    private static ApiTokenConfig instance;
    private final Properties properties;

    private ApiTokenConfig() {
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Get singleton instance of ApiTokenConfig
     */
    public static synchronized ApiTokenConfig getInstance() {
        if (instance == null) {
            instance = new ApiTokenConfig();
        }
        return instance;
    }

    /**
     * Load properties from the configuration file
     */
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                logger.warn("Unable to find {} in classpath. API token authentication will not be available.", PROPERTIES_FILE);
                return;
            }
            properties.load(input);
            logger.info("Successfully loaded REST work item configuration from {}", PROPERTIES_FILE);
        } catch (IOException ex) {
            logger.error("Error loading properties file: {}", PROPERTIES_FILE, ex);
        }
    }

    /**
     * Get the API token from properties
     * 
     * @return API token or null if not configured
     */
    public String getApiToken() {
        String token = properties.getProperty(API_TOKEN_KEY);
        if (token != null && !token.trim().isEmpty()) {
            return token.trim();
        }
        return null;
    }

    /**
     * Get the token header name
     * 
     * @return Header name (default: "Authorization")
     */
    public String getTokenHeader() {
        String header = properties.getProperty(TOKEN_HEADER_KEY);
        return (header != null && !header.trim().isEmpty()) ? header.trim() : DEFAULT_HEADER;
    }

    /**
     * Get the token prefix
     * 
     * @return Token prefix (default: "Bearer ")
     */
    public String getTokenPrefix() {
        String prefix = properties.getProperty(TOKEN_PREFIX_KEY);
        return (prefix != null) ? prefix : DEFAULT_PREFIX;
    }

    /**
     * Check if API token is configured
     * 
     * @return true if token is available, false otherwise
     */
    public boolean isTokenConfigured() {
        return getApiToken() != null;
    }

    /**
     * Get the full authorization header value (prefix + token)
     * 
     * @return Full authorization value or null if token not configured
     */
    public String getAuthorizationValue() {
        String token = getApiToken();
        if (token == null) {
            return null;
        }
        String prefix = getTokenPrefix();
        return prefix + token;
    }
}

// Made with Bob
