/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import org.activiti.cloud.api.process.model.CloudServiceTask;

public class CloudServiceTaskImpl extends CloudBPMNActivityImpl implements CloudServiceTask {

    private Integer integrationContextCounter;

    @Override
    public Integer getIntegrationContextCounter() {
        return integrationContextCounter;
    }

    public void setIntegrationContextCounter(Integer integrationContextCounter) {
        this.integrationContextCounter = integrationContextCounter;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("CloudServiceTaskImpl [integrationContextCounter=")
            .append(integrationContextCounter)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
