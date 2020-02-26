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
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
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
        CloudProcessStartedEventImpl event = buildProcessStartedEvent();

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

    private CloudProcessStartedEventImpl buildProcessStartedEvent() {
        CloudProcessStartedEventImpl cloudAuditEventEntity = new CloudProcessStartedEventImpl("ProcessStartedEventId",
                                                                                              System.currentTimeMillis(),
                                                                                              new ProcessInstanceImpl());
        cloudAuditEventEntity.setAppName("app");
        cloudAuditEventEntity.setAppVersion("v2");
        cloudAuditEventEntity.setServiceName("service");
        cloudAuditEventEntity.setServiceFullName("fullService");
        cloudAuditEventEntity.setServiceType("rb");
        cloudAuditEventEntity.setServiceVersion("sv1");
        cloudAuditEventEntity.setMessageId("msgId");
        cloudAuditEventEntity.setSequenceNumber(2);
        return cloudAuditEventEntity;
    }

    @Test
    public void convertToEntityShouldReturnCreatedEntity() {
        //given
        ProcessStartedAuditEventEntity auditEventEntity = new ProcessStartedAuditEventEntity();
        CloudProcessStartedEventImpl cloudRuntimeEvent = new CloudProcessStartedEventImpl();
        doReturn(auditEventEntity).when(eventConverter).createEventEntity(cloudRuntimeEvent);

        //when
        AuditEventEntity convertedEntity = eventConverter.convertToEntity(cloudRuntimeEvent);

        //then
        assertThat(convertedEntity).isEqualTo(auditEventEntity);
    }

    @Test
    public void createAPIEventShouldSetAllNonProcessContextRelatedFields() {
        //given
        CloudProcessStartedEventImpl cloudAuditEventEntity = buildProcessStartedEvent();
        
        ProcessStartedAuditEventEntity auditEventEntity = new ProcessStartedAuditEventEntity(cloudAuditEventEntity);
  
        //when
        ProcessStartedEventConverter converter = new ProcessStartedEventConverter(new EventContextInfoAppender());
        
        CloudProcessStartedEventImpl apiEvent = (CloudProcessStartedEventImpl)converter.convertToAPI(auditEventEntity);
        assertThat(apiEvent)
                .isNotNull()
                .isInstanceOf(CloudProcessStartedEvent.class);
        assertThat(apiEvent.getId()).isEqualTo(auditEventEntity.getEventId());
        assertThat(apiEvent.getTimestamp()).isEqualTo(auditEventEntity.getTimestamp());
        assertThat(apiEvent.getEntity()).isEqualTo(auditEventEntity.getProcessInstance());
        assertThat(apiEvent.getAppName()).isEqualTo(auditEventEntity.getAppName());
        assertThat(apiEvent.getAppVersion()).isEqualTo(auditEventEntity.getAppVersion());
        assertThat(apiEvent.getServiceFullName()).isEqualTo(auditEventEntity.getServiceFullName());
        assertThat(apiEvent.getServiceName()).isEqualTo(auditEventEntity.getServiceName());
        assertThat(apiEvent.getServiceType()).isEqualTo(auditEventEntity.getServiceType());
        assertThat(apiEvent.getServiceVersion()).isEqualTo(auditEventEntity.getServiceVersion());
        assertThat(apiEvent.getMessageId()).isEqualTo(auditEventEntity.getMessageId());
        assertThat(apiEvent.getSequenceNumber()).isEqualTo(auditEventEntity.getSequenceNumber());
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