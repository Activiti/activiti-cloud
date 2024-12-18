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
