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

package org.activiti.cloud.services.audit.jpa.events;

import java.util.Objects;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class IntegrationEventEntity extends AuditEventEntity {

    private String integrationContextId;

    private String flowNodeId;

    public String getIntegrationContextId() {
        return integrationContextId;
    }

    public void setIntegrationContextId(String integrationContextId) {
        this.integrationContextId = integrationContextId;
    }

    public String getFlowNodeId() {
        return flowNodeId;
    }

    public void setFlowNodeId(String flowNodeId) {
        this.flowNodeId = flowNodeId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(flowNodeId, integrationContextId);
        return result;
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
        IntegrationEventEntity other = (IntegrationEventEntity) obj;
        return Objects.equals(flowNodeId, other.flowNodeId) 
                && Objects.equals(integrationContextId, other.integrationContextId);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IntegrationEventEntity [integrationContextId=")
               .append(integrationContextId)
               .append(", flowNodeId=")
               .append(flowNodeId)
               .append(", toString()=")
               .append(super.toString())
               .append("]");
        return builder.toString();
    }

}
