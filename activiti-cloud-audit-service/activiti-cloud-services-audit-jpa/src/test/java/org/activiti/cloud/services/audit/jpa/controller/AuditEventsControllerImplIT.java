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

package org.activiti.cloud.services.audit.jpa.controller;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.api.process.model.BPMNMessage;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.events.BPMNMessageEvent;
import org.activiti.api.process.model.events.BPMNSignalEvent;
import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.TimerPayload;
import org.activiti.api.runtime.model.impl.BPMNMessageImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.BPMNTimerImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.audit.api.config.AuditAPIAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.conf.AuditJPAAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsControllerImpl;
import org.activiti.cloud.services.audit.jpa.events.ActivityStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageReceivedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageSentAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageWaitingAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.SignalReceivedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerFiredAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.security.config.AuditJPASecurityAutoConfiguration;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebMvcTest(AuditEventsControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import({
    AuditAPIAutoConfiguration.class,
    AuditJPAAutoConfiguration.class,
    AuditJPASecurityAutoConfiguration.class,
    AlfrescoWebAutoConfiguration.class
})
public class AuditEventsControllerImplIT {

    @MockBean
    private EventsRepository eventsRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockBean
    private UserGroupManager userGroupManager;

    @BeforeEach
    public void setUp() throws Exception {
        when(securityManager.getAuthenticatedUserId()).thenReturn("user");
    }

    @Test
    public void getEvents() throws Exception {
        PageRequest pageable = PageRequest.of(1,
                                              10);
        Page<AuditEventEntity> eventsPage = new PageImpl<>(buildEventsData(1),
                                                           pageable,
                                                           11);

        given(eventsRepository.findAll(any(),
                                       any(PageRequest.class))).willReturn(eventsPage);

        mockMvc.perform(get("{version}/events",
                            "/v1")
                                .param("page",
                                       "1")
                                .param("size",
                                       "10")
                                .param("sort",
                                       "asc"))
                .andExpect(status().isOk());
    }

    private List<AuditEventEntity> buildEventsData(int recordsNumber) {

        List<AuditEventEntity> eventsList = new ArrayList<>();

        for (long i = 0; i < recordsNumber; i++) {
            //would like to mock this but jackson and mockito not happy together
            AuditEventEntity eventEntity = buildAuditEventEntity(i);
            eventsList.add(eventEntity);
        }

        return eventsList;
    }

    private AuditEventEntity buildAuditEventEntity(long id) {
        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity();

        eventEntity.setEventId("eventId");
        eventEntity.setTimestamp(System.currentTimeMillis());
        eventEntity.setId(id);
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("10");
        processInstance.setProcessDefinitionId("1");
        eventEntity.setProcessInstance(processInstance);
        eventEntity.setServiceName("rb-my-app");
        eventEntity.setEventType(ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name());
        eventEntity.setProcessDefinitionId("1");
        eventEntity.setProcessInstanceId("10");
        eventEntity.setTimestamp(System.currentTimeMillis());
        return eventEntity;
    }

    @Test
    public void getEventsAlfresco() throws Exception {

        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11,
                                                                  10,
                                                                  PageRequest.of(0,
                                                                                 20));

        List<AuditEventEntity> events = buildEventsData(1);

        given(eventsRepository.findAll(any(),
                                       any(AlfrescoPageRequest.class)))
                .willReturn(new PageImpl<>(events,
                                           pageRequest,
                                           12));

        MvcResult result = mockMvc.perform(get("{version}/events?skipCount=11&maxItems=10",
                                               "/v1")
                                                   .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
                .node("list.pagination.skipCount").isEqualTo(11)
                .node("list.pagination.maxItems").isEqualTo(10)
                .node("list.pagination.count").isEqualTo(1)
                .node("list.pagination.hasMoreItems").isEqualTo(false)
                .node("list.pagination.totalItems").isEqualTo(12);
    }

    @Test
    public void headEvents() throws Exception {
        PageRequest pageable = PageRequest.of(1,
                                              10);
        Page<AuditEventEntity> eventsPage = new PageImpl<>(buildEventsData(1),
                                                           pageable,
                                                           10);

        given(eventsRepository.findAll(any(),
                                       any(PageRequest.class))).willReturn(eventsPage);

        mockMvc.perform(head("{version}/events",
                             "/v1"))
                .andExpect(status().isOk());
    }

    @Test
    public void headEventsAlfresco() throws Exception {
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11,
                                                                  10,
                                                                  PageRequest.of(0,
                                                                                 20));

        List<AuditEventEntity> events = buildEventsData(1);

        given(eventsRepository.findAll(any(),
                                       any(AlfrescoPageRequest.class)))
                .willReturn(new PageImpl<>(events,
                                           pageRequest,
                                           12));

        mockMvc.perform(head("{version}/events?skipCount=11&maxItems=10",
                             "/v1")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getEventById() throws Exception {

        AuditEventEntity eventEntity = buildAuditEventEntity(1);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        mockMvc.perform(get("{version}/events/{id}",
                            "/v1",
                            eventEntity.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void getEventByIdAlfresco() throws Exception {

        AuditEventEntity eventEntity = buildAuditEventEntity(1);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        mockMvc.perform(get("{version}/events/{id}",
                            "/v1",
                            eventEntity.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void headEventById() throws Exception {

        AuditEventEntity eventEntity = buildAuditEventEntity(1);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        AuditEventEntity event = new ActivityStartedAuditEventEntity();
        event.setEventId("eventId");
        event.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(head("{version}/events/{id}",
                             "/v1",
                             eventEntity.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void getSignalEventById() throws Exception {

        BPMNSignalImpl signal = new BPMNSignalImpl("elementId");
        signal.setSignalPayload(new SignalPayload("signal",
                                                  null));

        SignalReceivedAuditEventEntity eventEntity = new SignalReceivedAuditEventEntity();

        eventEntity.setEventId("eventId");
        eventEntity.setTimestamp(System.currentTimeMillis());
        eventEntity.setId(1L);
        eventEntity.setServiceName("rb-my-app");
        eventEntity.setEventType(BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED.name());
        eventEntity.setProcessDefinitionId("1");
        eventEntity.setProcessInstanceId("10");
        eventEntity.setSignal(signal);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        mockMvc.perform(get("{version}/events/{id}",
                            "/v1",
                            eventEntity.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetTimerEventById() throws Exception {

        BPMNTimerImpl timer = new BPMNTimerImpl("elementId");
        timer.setProcessDefinitionId("processDefinitionId");
        timer.setProcessInstanceId("processInstanceId");
        timer.setTimerPayload(createTimerPayload());

        TimerFiredAuditEventEntity eventEntity = new TimerFiredAuditEventEntity();
        eventEntity.setEventId("eventId");
        eventEntity.setTimestamp(System.currentTimeMillis());
        eventEntity.setEventType(BPMNTimerEvent.TimerEvents.TIMER_FIRED.name());

        eventEntity.setId(1L);
        eventEntity.setEntityId("entityId");
        eventEntity.setProcessInstanceId("processInstanceId");
        eventEntity.setProcessDefinitionId("processDefinitionId");
        eventEntity.setProcessDefinitionKey("processDefinitionKey");
        eventEntity.setBusinessKey("businessKey");
        eventEntity.setMessageId("message-id");
        eventEntity.setSequenceNumber(0);
        eventEntity.setTimer(timer);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        mockMvc.perform(get("{version}/events/{id}",
                            "/v1",
                            eventEntity.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetMessageSentEventById() throws Exception {
        MessageAuditEventEntity eventEntity = messageAuditEventEntity(MessageSentAuditEventEntity.class,
                                                                      BPMNMessageEvent.MessageEvents.MESSAGE_SENT);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        mockMvc.perform(get("{version}/events/{id}",
                            "/v1",
                            eventEntity.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetMessageWaitingEventById() throws Exception {
        MessageAuditEventEntity eventEntity = messageAuditEventEntity(MessageWaitingAuditEventEntity.class,
                                                                      BPMNMessageEvent.MessageEvents.MESSAGE_WAITING);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        mockMvc.perform(get("{version}/events/{id}",
                            "/v1",
                            eventEntity.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetMessageReceivedEventById() throws Exception {
        MessageAuditEventEntity eventEntity = messageAuditEventEntity(MessageReceivedAuditEventEntity.class,
                                                                      BPMNMessageEvent.MessageEvents.MESSAGE_RECEIVED);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        mockMvc.perform(get("{version}/events/{id}",
                            "/v1",
                            eventEntity.getId()))
                .andExpect(status().isOk());
    }

    private TimerPayload createTimerPayload() {
        TimerPayload timerPayload = new TimerPayload();
        timerPayload.setRetries(5);
        timerPayload.setMaxIterations(2);
        timerPayload.setRepeat("repeat");
        timerPayload.setExceptionMessage("Any message");

        return timerPayload;
    }

    private MessageAuditEventEntity messageAuditEventEntity(Class<? extends MessageAuditEventEntity> clazz,
                                                            BPMNMessageEvent.MessageEvents eventType) throws Exception {

        MessageAuditEventEntity eventEntity = clazz.newInstance();

        eventEntity.setEventId("eventId");
        eventEntity.setTimestamp(System.currentTimeMillis());
        eventEntity.setEventType(eventType.name());

        eventEntity.setId(1L);
        eventEntity.setEntityId("entityId");
        eventEntity.setProcessInstanceId("processInstanceId");
        eventEntity.setProcessDefinitionId("processDefinitionId");
        eventEntity.setProcessDefinitionKey("processDefinitionKey");
        eventEntity.setBusinessKey("businessKey");
        eventEntity.setMessageId("message-id");
        eventEntity.setSequenceNumber(0);
        eventEntity.setMessage(createBPMNMessage());

        return eventEntity;
    }


    private BPMNMessage createBPMNMessage() {
        BPMNMessageImpl message = new BPMNMessageImpl("elementId");
        message.setProcessDefinitionId("processDefinitionId");
        message.setProcessInstanceId("processInstanceId");
        message.setMessagePayload(createMessagePayload());

        return message;
    }


    private MessageEventPayload createMessagePayload() {
        MessageEventPayload messageEventPayload = MessagePayloadBuilder.event("messageName")
                                                                       .withBusinessKey("businessId")
                                                                       .withCorrelationKey("correlationId")
                                                                       .withVariable("name", "value")
                                                                       .build();

        return messageEventPayload;
    }
}
