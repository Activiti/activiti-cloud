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

package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessStartedEventConverterTest {

    @Spy
    @InjectMocks
    private ProcessStartedEventConverter eventConverter;

    @Mock
    private EventContextInfoAppender eventContextInfoAppender;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void createEventEntityShouldSetAllNonProcessContextRelatedFields() {
        //given
        CloudProcessStartedEventImpl event = new CloudProcessStartedEventImpl("ProcessStartedEventId",
                                                                               System.currentTimeMillis(),
                                                                              new ProcessInstanceImpl());
        event.setEntityId("entityId");
        event.setMessageId("message-id");
        event.setSequenceNumber(0);

        //when
        ProcessStartedAuditEventEntity auditEventEntity = eventConverter.createEventEntity(event);
     
        //then
        assertThat(auditEventEntity).isNotNull();
        assertThat(auditEventEntity.getEventId()).isEqualTo(event.getId());
        assertThat(auditEventEntity.getTimestamp()).isEqualTo(event.getTimestamp());
        assertThat(auditEventEntity.getAppName()).isEqualTo(event.getAppName());
        assertThat(auditEventEntity.getAppVersion()).isEqualTo(event.getAppVersion());
        assertThat(auditEventEntity.getServiceName()).isEqualTo(event.getServiceName());
        assertThat(auditEventEntity.getServiceFullName()).isEqualTo(event.getServiceFullName());
        assertThat(auditEventEntity.getServiceType()).isEqualTo(event.getServiceType());
        assertThat(auditEventEntity.getServiceVersion()).isEqualTo(event.getServiceVersion());
        assertThat(auditEventEntity.getMessageId()).isEqualTo(event.getMessageId());
        assertThat(auditEventEntity.getSequenceNumber()).isEqualTo(event.getSequenceNumber());
        assertThat(auditEventEntity.getProcessInstance()).isEqualTo(event.getEntity());
    }

    @Test
    public void convertToEntityShouldCreateEntityAndCallEventContextInfoAppender() {
        //given
        ProcessStartedAuditEventEntity auditEventEntity = new ProcessStartedAuditEventEntity();
        CloudProcessStartedEventImpl cloudRuntimeEvent = new CloudProcessStartedEventImpl();
        doReturn(auditEventEntity).when(eventConverter).createEventEntity(cloudRuntimeEvent);

        ProcessStartedAuditEventEntity updatedAuditEntity = new ProcessStartedAuditEventEntity();
        given(eventContextInfoAppender.addProcessContextInfoToEntityEvent(auditEventEntity, cloudRuntimeEvent)).willReturn(updatedAuditEntity);

        //when
        AuditEventEntity convertedEntity = eventConverter.convertToEntity(cloudRuntimeEvent);

        //then
        assertThat(convertedEntity).isEqualTo(updatedAuditEntity);
        verify(eventContextInfoAppender).addProcessContextInfoToEntityEvent(auditEventEntity, cloudRuntimeEvent);
    }

    @Test
    public void createAPIEventShouldSetAllNonProcessContextRelatedFields() {
        //given
        ProcessStartedAuditEventEntity processStartedAuditEventEntity = new ProcessStartedAuditEventEntity("eventId",
                                                                                                           System.currentTimeMillis(),
                                                                                                           "app",
                                                                                                           "v2",
                                                                                                           "service",
                                                                                                           "fullService",
                                                                                                           "rb",
                                                                                                           "sv1",
                                                                                                           "msgId",
                                                                                                           2,
                                                                                                           new ProcessInstanceImpl());

        //when
        CloudRuntimeEventImpl<?, ?> apiEvent = eventConverter.createAPIEvent(processStartedAuditEventEntity);
        assertThat(apiEvent)
                .isNotNull()
                .isInstanceOf(CloudProcessStartedEvent.class);
        CloudProcessStartedEvent cloudProcessStartedEvent = (CloudProcessStartedEvent) apiEvent;
        assertThat(cloudProcessStartedEvent.getId()).isEqualTo(processStartedAuditEventEntity.getEventId());
        assertThat(cloudProcessStartedEvent.getTimestamp()).isEqualTo(processStartedAuditEventEntity.getTimestamp());
        assertThat(cloudProcessStartedEvent.getEntity()).isEqualTo(processStartedAuditEventEntity.getProcessInstance());
        assertThat(cloudProcessStartedEvent.getAppName()).isEqualTo(processStartedAuditEventEntity.getAppName());
        assertThat(cloudProcessStartedEvent.getAppVersion()).isEqualTo(processStartedAuditEventEntity.getAppVersion());
        assertThat(cloudProcessStartedEvent.getServiceFullName()).isEqualTo(processStartedAuditEventEntity.getServiceFullName());
        assertThat(cloudProcessStartedEvent.getServiceName()).isEqualTo(processStartedAuditEventEntity.getServiceName());
        assertThat(cloudProcessStartedEvent.getServiceType()).isEqualTo(processStartedAuditEventEntity.getServiceType());
        assertThat(cloudProcessStartedEvent.getServiceVersion()).isEqualTo(processStartedAuditEventEntity.getServiceVersion());
        assertThat(cloudProcessStartedEvent.getMessageId()).isEqualTo(processStartedAuditEventEntity.getMessageId());
        assertThat(cloudProcessStartedEvent.getSequenceNumber()).isEqualTo(processStartedAuditEventEntity.getSequenceNumber());

    }

    @Test
    public void convertToAPIShouldCreateAPIEventAndCallEventContextInfoAppender() {
        //given
        ProcessStartedAuditEventEntity auditEventEntity = new ProcessStartedAuditEventEntity();
        CloudProcessStartedEventImpl apiEvent = new CloudProcessStartedEventImpl();
        doReturn(apiEvent).when(eventConverter).createAPIEvent(auditEventEntity);

        CloudProcessStartedEventImpl updatedApiEvent = new CloudProcessStartedEventImpl();
        given(eventContextInfoAppender.addProcessContextInfoToApiEvent(apiEvent, auditEventEntity)).willReturn(updatedApiEvent);


        //when
        CloudRuntimeEvent convertedEvent = eventConverter.convertToAPI(auditEventEntity);

        //then
        assertThat(convertedEvent).isEqualTo(updatedApiEvent);
        verify(eventContextInfoAppender).addProcessContextInfoToApiEvent(apiEvent, auditEventEntity);
    }
}