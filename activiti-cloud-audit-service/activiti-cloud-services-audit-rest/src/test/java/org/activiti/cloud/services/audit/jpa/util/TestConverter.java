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
package org.activiti.cloud.services.audit.jpa.util;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;

public class TestConverter implements EventToEntityConverter<AuditEventEntity> {

    public static final String EVENT_TYPE = "TestEvent";

    @Override
    public String getSupportedEvent() {
        return EVENT_TYPE;
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new AuditEventEntity() {
            @Override
            public String getEventId() {
                return cloudRuntimeEvent.getId();
            }
        };
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity eventEntity) {
        return new CloudRuntimeEvent() {
            @Override
            public String getAppVersion() {
                return eventEntity.getAppVersion();
            }

            @Override
            public String getAppName() {
                return eventEntity.getAppName();
            }

            @Override
            public String getServiceName() {
                return eventEntity.getServiceName();
            }

            @Override
            public String getServiceFullName() {
                return eventEntity.getServiceFullName();
            }

            @Override
            public String getServiceType() {
                return eventEntity.getServiceType();
            }

            @Override
            public String getServiceVersion() {
                return eventEntity.getServiceVersion();
            }

            @Override
            public String getId() {
                return String.valueOf(eventEntity.getId());
            }

            @Override
            public Object getEntity() {
                return eventEntity;
            }

            @Override
            public Long getTimestamp() {
                return eventEntity.getTimestamp();
            }

            @Override
            public Enum<?> getEventType() {
                return null;
            }

            @Override
            public String getProcessInstanceId() {
                return eventEntity.getProcessInstanceId();
            }

            @Override
            public String getParentProcessInstanceId() {
                return eventEntity.getParentProcessInstanceId();
            }

            @Override
            public String getProcessDefinitionId() {
                return eventEntity.getProcessDefinitionId();
            }

            @Override
            public String getProcessDefinitionKey() {
                return eventEntity.getProcessDefinitionKey();
            }

            @Override
            public Integer getProcessDefinitionVersion() {
                return 1;
            }

            @Override
            public String getBusinessKey() {
                return eventEntity.getBusinessKey();
            }

            @Override
            public Integer getSequenceNumber() {
                return eventEntity.getSequenceNumber();
            }

            @Override
            public String getMessageId() {
                return eventEntity.getMessageId();
            }

            @Override
            public String getEntityId() {
                return eventEntity.getEntityId();
            }

            @Override
            public String getActor() {
                return "actor";
            }
        };
    }
}
