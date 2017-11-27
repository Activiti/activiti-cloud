/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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
    private String name;

    private RuntimeBundleEventsProperties eventsProperties = new RuntimeBundleEventsProperties();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
