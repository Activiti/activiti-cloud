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
package org.activiti.cloud.services.graphql.autoconfigure;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix="spring.activiti.cloud.services.notifications.graphql")
@Validated
public class ActivitiGraphQLWebProperties {

    /**
     * Enable or disable graphql module services.
     */
    private boolean enabled;

    /**
     * graphql query executor REST endpoint. Default value is /graphql
     */
    @NotBlank
    private String path = "/graphql";

    @Configuration
    @PropertySources({
        @PropertySource(value="classpath:META-INF/graphql.properties"),
        @PropertySource(value="classpath:graphql.properties", ignoreResourceNotFound=true)
    })
    @EnableConfigurationProperties(ActivitiGraphQLWebProperties.class)
    public static class AutoConfiguration {

    }

    /**
     * Default constructor
     */
    ActivitiGraphQLWebProperties() { }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the endpoint
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setPath(String path) {
        this.path = path;
    }

}
