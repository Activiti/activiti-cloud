/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.api.process.model.impl;

import java.util.Objects;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.StartMessageSubscription;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.process.model.CloudStartMessageDeploymentDefinition;

public class CloudStartMessageDeploymentDefinitionImpl
    extends CloudRuntimeEntityImpl
    implements CloudStartMessageDeploymentDefinition {

    private ProcessDefinition processDefinition;

    private StartMessageSubscription messageSubscription;

    private CloudStartMessageDeploymentDefinitionImpl(Builder builder) {
        this.processDefinition = builder.processDefinition;
        this.messageSubscription = builder.messageSubscription;
        super.setAppName(builder.appName);
        super.setAppVersion(builder.appVersion);
        super.setServiceName(builder.serviceName);
        super.setServiceFullName(builder.serviceFullName);
        super.setServiceType(builder.serviceType);
        super.setServiceVersion(builder.serviceVersion);
    }

    CloudStartMessageDeploymentDefinitionImpl() {}

    @Override
    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    @Override
    public StartMessageSubscription getMessageSubscription() {
        return messageSubscription;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageSubscription, processDefinition);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CloudStartMessageDeploymentDefinitionImpl other = (CloudStartMessageDeploymentDefinitionImpl) obj;
        return (
            Objects.equals(messageSubscription, other.messageSubscription) &&
            Objects.equals(processDefinition, other.processDefinition)
        );
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("CloudStartMessageDeploymentDefinitionImpl [processDefinition=")
            .append(processDefinition)
            .append(", messageSubscription=")
            .append(messageSubscription)
            .append("]");
        return builder.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link CloudStartMessageDeploymentDefinitionImpl}.
     */
    public static final class Builder {

        private ProcessDefinition processDefinition;
        private StartMessageSubscription messageSubscription;
        private String appName;
        private String appVersion;
        private String serviceName;
        private String serviceFullName;
        private String serviceType;
        private String serviceVersion;

        public Builder() {}

        /**
         * Builder method for processDefinition parameter.
         * @param processDefinition field to set
         * @return builder
         */
        public Builder withProcessDefinition(ProcessDefinition processDefinition) {
            this.processDefinition = processDefinition;
            return this;
        }

        /**
         * Builder method for messageEventSubscription parameter.
         * @param messageSubscription field to set
         * @return builder
         */
        public Builder withMessageSubscription(StartMessageSubscription messageSubscription) {
            this.messageSubscription = messageSubscription;
            return this;
        }

        /**
         * Builder method for appName parameter.
         * @param appName field to set
         * @return builder
         */
        public Builder withAppName(String appName) {
            this.appName = appName;
            return this;
        }

        /**
         * Builder method for appVersion parameter.
         * @param appVersion field to set
         * @return builder
         */
        public Builder withAppVersion(String appVersion) {
            this.appVersion = appVersion;
            return this;
        }

        /**
         * Builder method for serviceName parameter.
         * @param serviceName field to set
         * @return builder
         */
        public Builder withServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        /**
         * Builder method for serviceFullName parameter.
         * @param serviceFullName field to set
         * @return builder
         */
        public Builder withServiceFullName(String serviceFullName) {
            this.serviceFullName = serviceFullName;
            return this;
        }

        /**
         * Builder method for serviceType parameter.
         * @param serviceType field to set
         * @return builder
         */
        public Builder withServiceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        /**
         * Builder method for serviceVersion parameter.
         * @param serviceVersion field to set
         * @return builder
         */
        public Builder withServiceVersion(String serviceVersion) {
            this.serviceVersion = serviceVersion;
            return this;
        }

        /**
         * Builder method of the builder.
         * @return built class
         */
        public CloudStartMessageDeploymentDefinitionImpl build() {
            return new CloudStartMessageDeploymentDefinitionImpl(this);
        }
    }
}
