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

import org.activiti.cloud.api.process.model.events.CloudProcessUpdatedEvent;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity(name = ProcessUpdatedAuditEventEntity.PROCESS_UPDATED_EVENT)
@DiscriminatorValue(value = ProcessUpdatedAuditEventEntity.PROCESS_UPDATED_EVENT)
public class ProcessUpdatedAuditEventEntity extends ProcessAuditEventEntity {

    protected static final String PROCESS_UPDATED_EVENT = "ProcessUpdatedEvent";

    public ProcessUpdatedAuditEventEntity() {
    }

    public ProcessUpdatedAuditEventEntity(CloudProcessUpdatedEvent cloudEvent) {
        super(cloudEvent);
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
        builder.append("ProcessUpdatedAuditEventEntity [toString()=").append(super.toString()).append("]");
        return builder.toString();
    }
}
