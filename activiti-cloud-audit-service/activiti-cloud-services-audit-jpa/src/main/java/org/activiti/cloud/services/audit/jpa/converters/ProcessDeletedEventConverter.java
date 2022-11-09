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
package org.activiti.cloud.services.audit.jpa.converters;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessDeletedEvent;
import org.activiti.cloud.api.process.model.events.ExtendedCloudProcessRuntimeEvent.ExtendedCloudProcessRuntimeEvents;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessDeletedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventSpecificationsBuilder;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.repository.SearchOperation;
import org.activiti.cloud.services.audit.jpa.repository.SpecSearchCriteria;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;

public class ProcessDeletedEventConverter extends BaseEventToEntityConverter {

    private final String PROCESS_INSTANCE_ID = "processInstanceId";

    private final String TIMESTAMP = "timestamp";

    private final String MISSING_PROCESS_INSTANCE = "Process Instance %s not found";

    private final String INVALID_PROCESS_INSTANCE_STATE = "Process Instance %s is not in a valid state: %s";

    private final Set<String> VALID_EVENT_TYPES =
        Arrays.asList(CloudRuntimeEventType.PROCESS_CANCELLED, CloudRuntimeEventType.PROCESS_COMPLETED).stream().map(Enum::name)
            .collect(Collectors.toSet());

    private final EventsRepository eventsRepository;

    public ProcessDeletedEventConverter(EventsRepository eventsRepository, EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
        this.eventsRepository = eventsRepository;
    }

    @Override
    public String getSupportedEvent() {
        return ExtendedCloudProcessRuntimeEvents.PROCESS_DELETED.name();
    }

    @Override
    protected ProcessDeletedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        ProcessAuditEventEntity event = findEvent(cloudRuntimeEvent.getProcessInstanceId());
        return new ProcessDeletedAuditEventEntity(event, (CloudProcessDeletedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessDeletedAuditEventEntity processDeletedAuditEventEntity = (ProcessDeletedAuditEventEntity) auditEventEntity;

        return new CloudProcessDeletedEventImpl(processDeletedAuditEventEntity.getEventId(),
            processDeletedAuditEventEntity.getTimestamp(),
            processDeletedAuditEventEntity.getProcessInstance());
    }

    protected ProcessAuditEventEntity findEvent(String processInstanceId) {
        Specification<AuditEventEntity> specification = new EventSpecificationsBuilder().with(
            new SpecSearchCriteria(PROCESS_INSTANCE_ID, SearchOperation.EQUALITY, processInstanceId)).build();
        List<AuditEventEntity> events = eventsRepository.findAll(specification, Sort.by(Order.desc(TIMESTAMP)));
        AuditEventEntity lastEvent = events.stream().findFirst()
            .orElseThrow(() -> new IllegalStateException(String.format(MISSING_PROCESS_INSTANCE, processInstanceId)));
        if (VALID_EVENT_TYPES.contains(lastEvent.getEventType())) {
            return ProcessAuditEventEntity.class.cast(lastEvent);
        } else {
            throw new IllegalStateException(String.format(INVALID_PROCESS_INSTANCE_STATE, processInstanceId, lastEvent.getEventType()));
        }
    }
}
