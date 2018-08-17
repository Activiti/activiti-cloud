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

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsAdminControllerImpl;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsControllerImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.runtime.api.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(AuditEventsAdminControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class EventsEngineEventsAdminControllerIT {

    private static final String DOCUMENTATION_IDENTIFIER = "events-admin";
    private static final String DOCUMENTATION_ALFRESCO_IDENTIFIER = "events-admin-alfresco";

    @MockBean
    private EventsRepository eventsRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityManager securityManager;

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

        given(eventsRepository.findAll(any(PageRequest.class))).willReturn(eventsPage);

        mockMvc.perform(get("/admin/{version}/events",
                            "v1")
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
        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity("id",
                                                                                        System.currentTimeMillis());
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

        given(eventsRepository.findAll(any(AlfrescoPageRequest.class)))
                .willReturn(new PageImpl<>(events,
                                           pageRequest,
                                           12));

        MvcResult result = mockMvc.perform(get("/admin/{version}/events?skipCount=11&maxItems=10",
                                               "v1")
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

        given(eventsRepository.findAll(any(PageRequest.class))).willReturn(eventsPage);

        mockMvc.perform(head("/admin/{version}/events",
                             "v1"))
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

        given(eventsRepository.findAll(
                any(AlfrescoPageRequest.class)))
                .willReturn(new PageImpl<>(events,
                                           pageRequest,
                                           12));

        mockMvc.perform(head("/admin/{version}/events?skipCount=11&maxItems=10",
                             "v1")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document(DOCUMENTATION_ALFRESCO_IDENTIFIER + "/head/list"));
    }
}
