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
package org.activiti.cloud.services.query.rest;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessVariableHistoryRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ProcessInstanceVariableHistoryAdminController.class)
@Import(
    { QueryRestWebMvcAutoConfiguration.class, CommonModelAutoConfiguration.class, AlfrescoWebAutoConfiguration.class }
)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
class ProcessInstanceVariableHistoryAdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessVariableHistoryRepository historyRepository;

    @MockitoBean
    private VariableRepository variableRepository;

    @MockitoBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @MockitoBean
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockitoBean
    private ProcessDefinitionRepository processDefinitionRepository;

    @MockitoBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockitoBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private ProcessInstanceAdminService processInstanceAdminService;

    @MockitoBean
    private ProcessInstanceService processInstanceService;

    @MockitoBean
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        assertThat(historyRepository).isNotNull();
    }

    @Test
    void should_returnHistory_when_calledWithApplicationJson() throws Exception {
        var processInstanceId = UUID.randomUUID().toString();
        var pageRequest = new AlfrescoPageRequest(0, 10, PageRequest.of(0, 10));
        var entity = buildHistoryEntity(processInstanceId);

        given(
            historyRepository.findByProcessInstanceIdOrderByEventTimeAscSequenceNumberAsc(
                eq(processInstanceId),
                any(Pageable.class)
            )
        ).willReturn(new PageImpl<>(List.of(entity), pageRequest, 1));

        MvcResult result = mockMvc
            .perform(
                get("/admin/v1/process-instances/{processInstanceId}/variables/history", processInstanceId)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        var content = result.getResponse().getContentAsString();
        assertThatJson(content).inPath("list.pagination.totalItems").isEqualTo(1);
        assertThatJson(content).inPath("list.pagination.count").isEqualTo(1);
        assertThatJson(content)
            .inPath("list.entries[0].entry.variableName")
            .isEqualTo("myVar");
        assertThatJson(content)
            .inPath("list.entries[0].entry.value")
            .isEqualTo("hello");
    }

    @Test
    void should_returnHistory_when_calledWithHalJson() throws Exception {
        var processInstanceId = UUID.randomUUID().toString();
        var entity = buildHistoryEntity(processInstanceId);

        given(
            historyRepository.findByProcessInstanceIdOrderByEventTimeAscSequenceNumberAsc(
                eq(processInstanceId),
                any(Pageable.class)
            )
        ).willReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));

        mockMvc
            .perform(
                get(
                    "/admin/v1/process-instances/{processInstanceId}/variables/history?page=0&size=20",
                    processInstanceId
                ).accept(MediaTypes.HAL_JSON_VALUE)
            )
            .andExpect(status().isOk());
    }

    private ProcessVariableHistoryEntity buildHistoryEntity(String processInstanceId) {
        var entity = new ProcessVariableHistoryEntity();
        var fixedDate = new Date(0L);
        entity.setProcessInstanceId(processInstanceId);
        entity.setVariableName("myVar");
        entity.setType(String.class.getName());
        entity.setValue("hello");
        entity.setDeleted(false);
        entity.setEventTime(fixedDate);
        entity.setRecordCreateTime(fixedDate);
        entity.setSequenceNumber(1);
        return entity;
    }
}
