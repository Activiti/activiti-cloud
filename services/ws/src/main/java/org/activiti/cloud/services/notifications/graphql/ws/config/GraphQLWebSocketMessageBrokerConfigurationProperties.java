/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.notifications.graphql.ws.config;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix="spring.activiti.cloud.services.notifications.graphql.ws")
@Validated
public class GraphQLWebSocketMessageBrokerConfigurationProperties {

    /* Enable or disable GraphQL WS broker. Default is true */
    @NotNull
    private Boolean enabled;

    /* stomp broker relay host. Default is localhost */
    @NotEmpty
    private String relayHost;

    /* Stomp broker relay port. Default is 61613 */
    @NotNull
    private Integer relayPort;

    /* Stomp broker client login. Default is guest */
    @NotEmpty
    private String clientLogin;

    /* Stomp broker client passcode. Default is guest */
    @NotEmpty
    private String clientPasscode;

    /* Stomp broker system login. Default is guest */
    @NotEmpty
    private String systemLogin;

    /* Stomp broker system passcode. Default is guest */
    @NotEmpty
    private String systemPasscode;

    /* WebSockets endpoint. Default is /ws/graphql */
    @NotEmpty
    private String endpoint;

    /* Allowed origins. Default is '*' */
    @NotEmpty
    private String allowedOrigins;

    /* Maximum outbound channel message buffer count to clients. Default is 50 */
    @NotNull
    private Integer bufferCount;

    /* Fixed buffer timespan duration in ms to trigger sending outbound messages to clients. Default is 1000 */
    @NotNull
    private Integer bufferTimeSpanMs;

    @Configuration
    @PropertySource("classpath:META-INF/graphql-ws.properties")
    @PropertySource(value="classpath:graphql-ws.properties", ignoreResourceNotFound=true)
    @EnableConfigurationProperties(GraphQLWebSocketMessageBrokerConfigurationProperties.class)
    public static class AutoConfiguration {

    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getRelayHost() {
        return relayHost;
    }

    public void setRelayHost(String relayHost) {
        this.relayHost = relayHost;
    }

    public int getRelayPort() {
        return relayPort;
    }

    public void setRelayPort(int relayPort) {
        this.relayPort = relayPort;
    }

    public String getClientLogin() {
        return clientLogin;
    }

    public void setClientLogin(String relayLogin) {
        this.clientLogin = relayLogin;
    }

    public String getClientPasscode() {
        return clientPasscode;
    }

    public void setClientPasscode(String relayPasscode) {
        this.clientPasscode = relayPasscode;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }


    public Integer getBufferCount() {
        return bufferCount;
    }


    public void setBufferCount(Integer bufferCount) {
        this.bufferCount = bufferCount;
    }


    public Integer getBufferTimeSpanMs() {
        return bufferTimeSpanMs;
    }

    public void setBufferTimeSpanMs(Integer bufferTimeSpanMs) {
        this.bufferTimeSpanMs = bufferTimeSpanMs;
    }

    public String getSystemLogin() {
        return systemLogin;
    }

    public void setSystemLogin(String systemLogin) {
        this.systemLogin = systemLogin;
    }

    public String getSystemPasscode() {
        return systemPasscode;
    }

    public void setSystemPasscode(String systemPasscode) {
        this.systemPasscode = systemPasscode;
    }
}
