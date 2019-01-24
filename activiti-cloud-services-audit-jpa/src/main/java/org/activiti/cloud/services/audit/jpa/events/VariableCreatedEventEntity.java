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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.model.shared.model.VariableInstance;

@Entity
@DiscriminatorValue(value = VariableCreatedEventEntity.VARIABLE_CREATED_EVENT)
public class VariableCreatedEventEntity extends VariableAuditEventEntity {

    protected static final String VARIABLE_CREATED_EVENT = "VariableCreatedEvent";

    public VariableCreatedEventEntity() {
    }

    public VariableCreatedEventEntity(String eventId,
                                      Long timestamp) {
        super(eventId,
              timestamp,
              VariableEvent.VariableEvents.VARIABLE_CREATED.name());
    }

    public VariableCreatedEventEntity(String eventId,
                                      Long timestamp,
                                      String appName,
                                      String appVersion,
                                      String serviceName,
                                      String serviceFullName,
                                      String serviceType,
                                      String serviceVersion,
                                      String messageId,
                                      Integer sequenceNumber,
                                      VariableInstance variableInstance) {
        super(eventId,
              timestamp,
              VariableEvent.VariableEvents.VARIABLE_CREATED.name(),
              appName,
              appVersion,
              serviceName,
              serviceFullName,
              serviceType,
              serviceVersion,
              messageId,
              sequenceNumber,
              variableInstance);
    }
}
