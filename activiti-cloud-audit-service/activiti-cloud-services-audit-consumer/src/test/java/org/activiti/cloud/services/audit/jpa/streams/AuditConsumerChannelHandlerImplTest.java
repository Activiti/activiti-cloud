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
package org.activiti.cloud.services.audit.jpa.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.IncidentEvent.IncidentEventType;
import org.activiti.cloud.api.process.model.IncidentSeverity;
import org.activiti.cloud.api.process.model.impl.events.CloudIncidentCreatedEventImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.converters.EventContextInfoAppender;
import org.activiti.cloud.services.audit.jpa.converters.IncidentCreatedEventConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IncidentCreatedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCreatedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditConsumerChannelHandlerImplTest {

    @InjectMocks
    private AuditConsumerChannelHandlerImpl handler;

    @Mock
    private EventsRepository eventsRepository;

    @Mock
    private APIEventToEntityConverters converters;

    @Captor
    private ArgumentCaptor<Iterable<AuditEventEntity>> argumentCaptor;

    @Test
    void receiveEventShouldStoreEntity() {
        //given
        CloudRuntimeEvent cloudRuntimeEvent = mock(CloudRuntimeEventImpl.class);
        when(cloudRuntimeEvent.getEventType()).thenReturn(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED);
        EventToEntityConverter converter = mock(EventToEntityConverter.class);
        when(converters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name()))
            .thenReturn(converter);
        ProcessCreatedAuditEventEntity entity = mock(ProcessCreatedAuditEventEntity.class);
        when(converter.convertToEntity(cloudRuntimeEvent)).thenReturn(entity);

        CloudRuntimeEvent[] events = { cloudRuntimeEvent };

        //when
        handler.receiveCloudRuntimeEvent(
            new HashMap<String, Object>() {
                {
                    put("id", UUID.randomUUID());
                }
            },
            events
        );

        //then
        verify(eventsRepository).saveAll(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).containsOnly(entity);
    }

    @Test
    void messageIdShouldBeSet() {
        //given
        CloudRuntimeEvent cloudRuntimeEvent = mock(CloudRuntimeEventImpl.class);
        when(cloudRuntimeEvent.getEventType()).thenReturn(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED);
        EventToEntityConverter converter = mock(EventToEntityConverter.class);
        when(converters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name()))
            .thenReturn(converter);
        AuditEventEntity entity = mock(AuditEventEntity.class);
        when(converter.convertToEntity(cloudRuntimeEvent)).thenReturn(entity);

        CloudRuntimeEvent[] events = { cloudRuntimeEvent };

        HashMap<String, Object> headers = new HashMap<>();
        headers.put("id", UUID.randomUUID());

        //when
        handler.receiveCloudRuntimeEvent(headers, events);

        //then
        verify((CloudRuntimeEventImpl) cloudRuntimeEvent).setMessageId(headers.get("id").toString());
    }

    @Test
    void receiveCloudRuntimeEventIncidentsAndSetMessageIdAndSequenceNumber() {
        var eventContextInfoAppender = mock(EventContextInfoAppender.class);
        var converter = new IncidentCreatedEventConverter(eventContextInfoAppender);
        var incidentEvent = new CloudIncidentCreatedEventImpl(
            "incident-id",
            System.currentTimeMillis(),
            null,
            "ErrorClassName",
            "ERROR_CODE",
            "Error message",
            List.of(new StackTraceElement[0]),
            IncidentSeverity.WARNING
        );
        incidentEvent.setAppName("test-app");
        incidentEvent.setServiceName("test-service");

        when(converters.getConverterByEventTypeName(IncidentEventType.INCIDENT_CREATED.name())).thenReturn(converter);

        CloudRuntimeEvent[] events = { incidentEvent };
        HashMap<String, Object> headers = new HashMap<>();
        var messageId = UUID.randomUUID();
        headers.put("id", messageId);

        handler.receiveCloudRuntimeEvent(headers, events);

        verify(eventsRepository).saveAll(argumentCaptor.capture());
        IncidentCreatedEventEntity savedEntity = (IncidentCreatedEventEntity) argumentCaptor
            .getValue()
            .iterator()
            .next();

        assertThat(savedEntity.getMessageId()).isEqualTo(messageId.toString());
        assertThat(savedEntity.getSequenceNumber()).isEqualTo(0);
        assertThat(savedEntity.getErrorCode()).isEqualTo("ERROR_CODE");
        assertThat(savedEntity.getSeverity()).isEqualTo(IncidentSeverity.WARNING);
    }
}
