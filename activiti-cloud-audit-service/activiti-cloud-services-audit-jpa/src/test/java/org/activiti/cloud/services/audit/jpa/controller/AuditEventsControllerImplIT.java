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
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@WebMvcTest(AuditEventsControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({
    AuditAPIAutoConfiguration.class,
    AuditJPAAutoConfiguration.class,
    AuditJPASecurityAutoConfiguration.class,
    AlfrescoWebAutoConfiguration.class
})
public class AuditEventsControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "events";
    private static final String DOCUMENTATION_ALFRESCO_IDENTIFIER = "events-alfresco";

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

    @Before
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
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(
                                        subsectionWithPath("_embedded.events").description("A list of events "),
                                        subsectionWithPath("_links.self").description("Resource Self Link"),
                                        subsectionWithPath("_links.first").description("Pagination First Link"),
                                        subsectionWithPath("_links.prev").description("Pagination Prev Link"),
                                        subsectionWithPath("_links.last").description("Pagination Last Link"),
                                        subsectionWithPath("page").description("Pagination details."))));
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
                .andDo(document(DOCUMENTATION_ALFRESCO_IDENTIFIER + "/list",
                                pageRequestParameters(),
                                pagedResourcesResponseFields()
                ))
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
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/head/list"));
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
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document(DOCUMENTATION_ALFRESCO_IDENTIFIER + "/head/list"));
    }

    @Test
    public void getEventById() throws Exception {

        AuditEventEntity eventEntity = buildAuditEventEntity(1);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        mockMvc.perform(get("{version}/events/{id}",
                            "/v1",
                            eventEntity.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("id").description("The event id"),
                                               parameterWithName("version").description("The API version")),
                                responseFields(
                                        subsectionWithPath("id").description("The event id"),
                                        subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                        subsectionWithPath("timestamp").description("The event timestamp"),
                                        subsectionWithPath("eventType").description("The event type"),
                                        subsectionWithPath("appName").description("The application name"),
                                        subsectionWithPath("serviceFullName").description("The full service name"),
                                        subsectionWithPath("appVersion").description("The application version"),
                                        subsectionWithPath("serviceVersion").description("The version of the service"),
                                        subsectionWithPath("serviceType").description("The type of the service"),
                                        subsectionWithPath("nestedProcessDefinitionId").description("Nested process definition id"),
                                        subsectionWithPath("nestedProcessInstanceId").description("Nested process instance id"),
                                        subsectionWithPath("serviceName").description("The service name"),
                                        subsectionWithPath("entityId").description("the entity idCloudProcessSuspendedEventImpl"),
                                        subsectionWithPath("entity").description("the process instance entity"),
                                        subsectionWithPath("entity.processDefinitionId").description("The process definition id"),
                                        subsectionWithPath("entity.id").description("The associated entity id"),
                                        subsectionWithPath("processInstanceId").description("The process instance id"),
                                        subsectionWithPath("processDefinitionId").description("The process definition id"),
                                        subsectionWithPath("processDefinitionKey").description("The process definition key"),
                                        subsectionWithPath("processDefinitionVersion").description("The process definition version"),
                                        subsectionWithPath("businessKey").description("The businessKey"),
                                        subsectionWithPath("parentProcessInstanceId").description("The parent process instance id"),
                                        subsectionWithPath("messageId").description("The transaction id coming from received message"),
                                        subsectionWithPath("sequenceNumber").description("The position of the event within the transaction")

                                )));
    }

    @Test
    public void getEventByIdAlfresco() throws Exception {

        AuditEventEntity eventEntity = buildAuditEventEntity(1);

        given(eventsRepository.findByEventId(anyString())).willReturn(Optional.of(eventEntity));

        mockMvc.perform(get("{version}/events/{id}",
                            "/v1",
                            eventEntity.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_ALFRESCO_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("id").description("The event id"),
                                               parameterWithName("version").description("The API version")),
                                responseFields(
                                        subsectionWithPath("entry").ignored(),
                                        subsectionWithPath("entry.id").description("The event id"),
                                        subsectionWithPath("entry.timestamp").description("The event timestamp"),
                                        subsectionWithPath("entry.eventType").description("The event type"),
                                        subsectionWithPath("entry.entity.processDefinitionId").description("The process definition id"),
                                        subsectionWithPath("entry.entity.id").description("The process instance id"),
                                        subsectionWithPath("entry.serviceName").description("The service name"),
                                        subsectionWithPath("entry.messageId").description("The transaction id coming from received message"),
                                        subsectionWithPath("entry.sequenceNumber").description("The position of the event within the transaction")

                                )));
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
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/head"));
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
                .andDo(print())
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
                .andDo(print())
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
                .andDo(print())
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
                .andDo(print())
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
                .andDo(print())
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
