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
package org.activiti.cloud.services.notifications.graphql.events.consumer;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix="spring.activiti.cloud.services.notifications.graphql.events")
public class EngineEventsConsumerProperties {

    /**
     * Enable or disable notification module services.
     */
    private boolean enabled;

    /**
     * Customizes common process engine event attributes using comma separator
     */
    @NotBlank
    private String processEngineEventAttributeKeys = "serviceName,appName,processDefinitionKey,processInstanceId,businessKey";

    /**
     * Customizes eventType key attribute name
     */
    @NotBlank
    private String processEngineEventTypeKey = "eventType";


    public EngineEventsConsumerProperties() {
        // default constructor
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProcessEngineEventAttributeKeys() {
        return processEngineEventAttributeKeys;
    }

    public void setProcessEngineEventAttributeKeys(String processEngineEventAttributeKeys) {
        this.processEngineEventAttributeKeys = processEngineEventAttributeKeys;
    }

    public String getProcessEngineEventTypeKey() {
        return processEngineEventTypeKey;
    }

    public void setProcessEngineEventTypeKey(String processEngineEventTypeKey) {
        this.processEngineEventTypeKey = processEngineEventTypeKey;
    }
}
