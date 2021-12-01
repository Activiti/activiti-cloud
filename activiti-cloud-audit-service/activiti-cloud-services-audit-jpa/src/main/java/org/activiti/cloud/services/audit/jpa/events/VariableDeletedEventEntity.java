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
package org.activiti.cloud.services.audit.jpa.events;

import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity(name = VariableDeletedEventEntity.VARIABLE_DELETED_EVENT)
@DiscriminatorValue(value = VariableDeletedEventEntity.VARIABLE_DELETED_EVENT)
public class VariableDeletedEventEntity extends VariableAuditEventEntity {

    protected static final String VARIABLE_DELETED_EVENT = "VariableDeletedEvent";

    public VariableDeletedEventEntity() {
    }

    public VariableDeletedEventEntity(CloudVariableDeletedEvent cloudEvent) {
        super(cloudEvent);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("VariableDeletedEventEntity [toString()=").append(super.toString()).append("]");
        return builder.toString();
    }
}
