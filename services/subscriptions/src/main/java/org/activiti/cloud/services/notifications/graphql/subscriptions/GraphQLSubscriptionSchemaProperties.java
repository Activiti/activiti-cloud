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
package org.activiti.cloud.services.notifications.graphql.subscriptions;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix="org.activiti.cloud.services.notifications.graphql.subscriptions")
public class GraphQLSubscriptionSchemaProperties {

    /**
     * The URL of GraphQL subscription schema file name with .graphqls extension. Defaults to classpath:activiti.graphqls
     */
    @NotBlank
    private String graphqls;

    /**
     * GraphQL subscription schema field name. Defaults to ProcessEngineNotification
     */
    @NotBlank
    private String subscriptionFieldName;

    /**
     * GraphQL subscription field comma-separated list of argument names to build hierarchical Stomp destination subscription topic.
     * Defaults to serviceName,appName,processDefinitionId,processInstanceId
     */
    @NotNull
    private String[] subscriptionArgumentNames;

    @NotBlank
    private String relayHost;

    @NotNull
    private Integer relayPort;

    @NotBlank
    private String clientLogin;

    @NotBlank
    private String clientPasscode;

    @Configuration
    @PropertySource("classpath:META-INF/graphql-subscriptions.properties")
    @PropertySource(value = "classpath:graphql-subscriptions.properties", ignoreResourceNotFound = true)
    @EnableConfigurationProperties(GraphQLSubscriptionSchemaProperties.class)
    public static class AutoConfiguration {
        // auto configures parent properties class using spring.factories
    }

    public String getGraphqls() {
        return graphqls;
    }

    public void setGraphqls(String graphqls) {
        this.graphqls = graphqls;
    }

    public String getSubscriptionFieldName() {
        return subscriptionFieldName;
    }

    public void setSubscriptionFieldName(String subscriptionFieldName) {
        this.subscriptionFieldName = subscriptionFieldName;
    }

    public String[] getSubscriptionArgumentNames() {
        return subscriptionArgumentNames;
    }

    public void setSubscriptionArgumentNames(String[] argumentNames) {
        this.subscriptionArgumentNames = argumentNames;
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
    

}
