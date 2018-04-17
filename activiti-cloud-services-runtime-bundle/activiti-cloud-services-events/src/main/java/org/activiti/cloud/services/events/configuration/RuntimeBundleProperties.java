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

package org.activiti.cloud.services.events.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "activiti.cloud.runtime-bundle")
public class RuntimeBundleProperties {

    @Value("${spring.application.name}")
    private String rbSpringAppName;

    @Value("${activiti.cloud.service.type:}")
    private String serviceType;

    @Value("${activiti.cloud.service.version:}")
    private String serviceVersion;

    @Value("${activiti.cloud.application.name:}")
    private String activitiAppName;

    @Value("${activiti.cloud.application.version:}")
    private String activitiAppVersion;

    private RuntimeBundleEventsProperties eventsProperties = new RuntimeBundleEventsProperties();

    public String getRbSpringAppName() {
        return rbSpringAppName;
    }

    public String getFullyQualifiedServiceName(){
        //if we change this then we also have to change integration-result-stream.properties
        return rbSpringAppName;
    }

    // a level of indirection here as we may change this to use its own property
    public String getServiceName(){
        return getRbSpringAppName();
    }

    public void setRbSpringAppName(String name) {
        this.rbSpringAppName = name;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getActivitiAppName() {
        return activitiAppName;
    }

    public void setActivitiAppName(String activitiAppName) {
        this.activitiAppName = activitiAppName;
    }

    public String getActivitiAppVersion() {
        return activitiAppVersion;
    }

    public void setActivitiAppVersion(String activitiAppVersion) {
        this.activitiAppVersion = activitiAppVersion;
    }

    public RuntimeBundleEventsProperties getEventsProperties() {
        return eventsProperties;
    }

    public void setEventsProperties(RuntimeBundleEventsProperties eventsProperties) {
        this.eventsProperties = eventsProperties;
    }

    public static class RuntimeBundleEventsProperties {

        private boolean integrationAuditEventsEnabled;

        public boolean isIntegrationAuditEventsEnabled() {
            return integrationAuditEventsEnabled;
        }

        public void setIntegrationAuditEventsEnabled(boolean integrationAuditEventsEnabled) {
            this.integrationAuditEventsEnabled = integrationAuditEventsEnabled;
        }
    }
}
