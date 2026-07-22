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
package org.activiti.cloud.services.audit.jpa.controller.v2;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.function.Function;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.audit.api.config.AuditAPIAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.assembler.config.EventRepresentationModelAssemblerConfiguration;
import org.activiti.cloud.services.audit.jpa.conf.AuditJPAAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.controllers.config.AuditJPAControllersAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.controllers.v2.AuditEventsAdminControllerV2Impl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.security.config.AuditJPASecurityAutoConfiguration;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor.SpecificationFluentQuery;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuditEventsAdminControllerV2Impl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import(
    {
        EventRepresentationModelAssemblerConfiguration.class,
        AuditAPIAutoConfiguration.class,
        AuditJPAAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class,
        AuditJPAControllersAutoConfiguration.class,
        AuditJPASecurityAutoConfiguration.class,
    }
)
class AuditEventsAdminControllerV2ImplWebMvcTest {

    @MockitoBean
    private EventsRepository eventsRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private UserGroupManager userGroupManager;

    @MockitoBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @BeforeEach
    void setUp() {
        when(securityManager.getAuthenticatedUserId()).thenReturn("user");
    }

    @Test
    void getAdminEvents() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        givenSlice(new SliceImpl<>(buildEventsData(1), pageable, false));

        mockMvc
            .perform(get("/admin/{version}/events", "v2").param("page", "0").param("size", "10").param("sort", "asc"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldDefaultSortToTimestampDescWhenNoSortProvided() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        ArgumentCaptor<Pageable> pageableCaptor = givenSlice(new SliceImpl<>(buildEventsData(1), pageable, false));

        mockMvc.perform(get("/admin/{version}/events", "v2")).andExpect(status().isOk());

        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.isSorted()).isTrue();
        assertThat(sort).containsExactly(new Sort.Order(Sort.Direction.DESC, "timestamp"));
    }

    @Test
    void getAdminEventsAlfresco() throws Exception {
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(0, 10, PageRequest.of(0, 20));
        givenSlice(new SliceImpl<>(buildEventsData(1), pageRequest, false));

        MvcResult result = mockMvc
            .perform(get("/admin/{version}/events?skipCount=0&maxItems=10", "v2").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.skipCount").isEqualTo(0);
        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.count").isEqualTo(1);
        assertThatJson(result.getResponse().getContentAsString())
            .inPath("list.pagination.hasMoreItems")
            .isEqualTo(false);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Pageable> givenSlice(Slice<AuditEventEntity> slice) {
        SpecificationFluentQuery<AuditEventEntity> fluentQuery = mock(SpecificationFluentQuery.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(fluentQuery.slice(pageableCaptor.capture())).willReturn(slice);
        given(eventsRepository.findBy(any(Specification.class), any())).willAnswer(invocation -> {
            Function<SpecificationFluentQuery<AuditEventEntity>, Object> queryFunction = invocation.getArgument(1);
            return queryFunction.apply(fluentQuery);
        });
        return pageableCaptor;
    }

    private List<AuditEventEntity> buildEventsData(int recordsNumber) {
        List<AuditEventEntity> eventsList = new java.util.ArrayList<>();
        for (long i = 0; i < recordsNumber; i++) {
            eventsList.add(buildAuditEventEntity(i));
        }
        return eventsList;
    }

    private AuditEventEntity buildAuditEventEntity(long id) {
        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity();
        eventEntity.setEventId("eventId");
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
}
