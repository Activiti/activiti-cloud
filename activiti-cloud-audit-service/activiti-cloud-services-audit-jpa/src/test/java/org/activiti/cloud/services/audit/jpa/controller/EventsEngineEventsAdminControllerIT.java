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
package org.activiti.cloud.services.audit.jpa.controller;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.audit.api.config.AuditAPIAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.conf.AuditJPAAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsAdminControllerImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableCreatedEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuditEventsAdminControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import({ AuditAPIAutoConfiguration.class, AuditJPAAutoConfiguration.class, AlfrescoWebAutoConfiguration.class })
class EventsEngineEventsAdminControllerIT {

    private static final String HEADER_ATTACHMENT_FILENAME = "attachment;filename=";
    private static final String CSV_FILENAME = "20220710_testApp_audit.csv";

    private static String CSV_CONTENT =
        """
        "ACTOR","APPNAME","APPVERSION","BUSINESSKEY","ENTITY","ENTITYID","EVENTTYPE","ID","MESSAGEID","PARENTPROCESSINSTANCEID","PROCESSDEFINITIONID","PROCESSDEFINITIONKEY","PROCESSDEFINITIONVERSION","PROCESSINSTANCEID","SEQUENCENUMBER","SERVICEFULLNAME","SERVICENAME","SERVICETYPE","SERVICEVERSION","TIME"
        "service_user","testApp","","","{""appVersion"":null,""id"":""10"",""name"":null,""processDefinitionId"":""1"",""processDefinitionKey"":null,""initiator"":null,""startDate"":null,""completedDate"":null,""businessKey"":null,""status"":null,""parentId"":null,""processDefinitionVersion"":null,""processDefinitionName"":null}","","PROCESS_STARTED","processEventId","","","1","","","10","0","","rb-my-app","","","2022-07-07 14:59:37"
        "service_user","testApp","","","{""name"":""var"",""type"":null,""processInstanceId"":""processId"",""value"":null,""taskId"":""taskId"",""taskVariable"":true}","var","VARIABLE_CREATED","variableEventId","","","1","","","10","0","","rb-my-app","","","2022-07-07 14:59:37"
        """;

    @MockBean
    private EventsRepository eventsRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private UserGroupManager userGroupManager;

    @BeforeEach
    void setUp() throws Exception {
        when(securityManager.getAuthenticatedUserId()).thenReturn("user");
    }

    @Test
    void getEvents() throws Exception {
        PageRequest pageable = PageRequest.of(1, 10);
        Page<AuditEventEntity> eventsPage = new PageImpl<>(buildEventsData(1), pageable, 11);

        given(eventsRepository.findAll(any(PageRequest.class))).willReturn(eventsPage);

        mockMvc
            .perform(get("/admin/{version}/events", "v1").param("page", "1").param("size", "10").param("sort", "asc"))
            .andExpect(status().isOk());
    }

    @Test
    void exportEvents() throws Exception {
        List<AuditEventEntity> events = buildEventsData(1);
        events.add(buildVariableAuditEventEntity(2));

        given(eventsRepository.findAllByOrderByTimestampDesc()).willReturn(events);

        MvcResult response = mockMvc
            .perform(get("/admin/{version}/events/export/" + CSV_FILENAME, "v1"))
            .andExpect(status().isOk())
            .andReturn();

        assertCsv(response.getResponse(), CSV_CONTENT);
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
        eventEntity.setAppName("testApp");
        eventEntity.setEventId("processEventId");
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
        eventEntity.setTimestamp(1657205977551L);
        return eventEntity;
    }

    private AuditEventEntity buildVariableAuditEventEntity(long id) {
        VariableCreatedEventEntity eventEntity = new VariableCreatedEventEntity();
        eventEntity.setAppName("testApp");
        eventEntity.setEventId("variableEventId");
        eventEntity.setTimestamp(System.currentTimeMillis());
        eventEntity.setId(id);
        eventEntity.setVariableInstance(new VariableInstanceImpl<Object>("var", null, null, "processId", "taskId"));
        eventEntity.setServiceName("rb-my-app");
        eventEntity.setEventType(VariableEvent.VariableEvents.VARIABLE_CREATED.name());
        eventEntity.setProcessDefinitionId("1");
        eventEntity.setProcessInstanceId("10");
        eventEntity.setTimestamp(1657205977551L);
        return eventEntity;
    }

    @Test
    void getEventsAlfresco() throws Exception {
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11, 10, PageRequest.of(0, 20));

        List<AuditEventEntity> events = buildEventsData(1);

        given(eventsRepository.findAll(any(AlfrescoPageRequest.class)))
            .willReturn(new PageImpl<>(events, pageRequest, 12));

        MvcResult result = mockMvc
            .perform(get("/admin/{version}/events?skipCount=11&maxItems=10", "v1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
            .node("list.pagination.skipCount")
            .isEqualTo(11)
            .node("list.pagination.maxItems")
            .isEqualTo(10)
            .node("list.pagination.count")
            .isEqualTo(1)
            .node("list.pagination.hasMoreItems")
            .isEqualTo(false)
            .node("list.pagination.totalItems")
            .isEqualTo(12);
    }

    @Test
    void headEvents() throws Exception {
        PageRequest pageable = PageRequest.of(1, 10);
        Page<AuditEventEntity> eventsPage = new PageImpl<>(buildEventsData(1), pageable, 10);

        given(eventsRepository.findAll(any(PageRequest.class))).willReturn(eventsPage);

        mockMvc.perform(head("/admin/{version}/events", "v1")).andExpect(status().isOk());
    }

    @Test
    void headEventsAlfresco() throws Exception {
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11, 10, PageRequest.of(0, 20));

        List<AuditEventEntity> events = buildEventsData(1);

        given(eventsRepository.findAll(any(AlfrescoPageRequest.class)))
            .willReturn(new PageImpl<>(events, pageRequest, 12));

        mockMvc
            .perform(head("/admin/{version}/events?skipCount=11&maxItems=10", "v1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    private void assertCsv(MockHttpServletResponse response, String expectedContent) {
        assertThat(response).isNotNull();
        assertThat(response.getContentType()).isNotEmpty();
        assertThat(response.getContentType()).isEqualTo("text/csv");
        byte[] contentBytes = response.getContentAsByteArray();
        assertThat(contentBytes).isNotEmpty();

        String contentDispositionHeader = response.getHeader(CONTENT_DISPOSITION);

        assertThat(contentDispositionHeader).isNotEmpty().startsWith(HEADER_ATTACHMENT_FILENAME);

        String fileName = contentDispositionHeader.substring(HEADER_ATTACHMENT_FILENAME.length());
        assertThat(fileName).isEqualTo(CSV_FILENAME);
        assertThat(new String(contentBytes)).isEqualTo(expectedContent);
    }
}
