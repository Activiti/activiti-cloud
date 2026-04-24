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

import com.querydsl.core.types.Predicate;
import jakarta.persistence.EntityManagerFactory;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
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
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(TaskVariableController.class)
@Import(
    { QueryRestWebMvcAutoConfiguration.class, CommonModelAutoConfiguration.class, AlfrescoWebAutoConfiguration.class }
)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
public class TaskEntityVariableEntityControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskVariableRepository variableRepository;

    @MockitoBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private VariableRepository processVariableRepository;

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
        assertThat(processInstanceAdminService).isNotNull();
        assertThat(processInstanceService).isNotNull();
        assertThat(entityManagerFactory).isNotNull();
    }

    @Test
    public void getVariablesShouldReturnAllResultsUsingAlfrescoMetadataWhenMediaTypeIsApplicationJson()
        throws Exception {
        //given
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11, 10, PageRequest.of(0, 20));
        TaskVariableEntity variableEntity = buildVariable();
        given(variableRepository.findAll(any(Predicate.class), eq(pageRequest)))
            .willReturn(new PageImpl<>(Collections.singletonList(variableEntity), pageRequest, 12));

        //when
        MvcResult result = mockMvc
            .perform(
                get("/v1/tasks/{taskId}/variables?skipCount=11&maxItems=10", variableEntity.getTaskId())
                    .accept(MediaType.APPLICATION_JSON)
            )
            //then
            .andExpect(status().isOk())
            .andReturn();

        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.skipCount").isEqualTo(11);
        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.maxItems").isEqualTo(10);
        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.count").isEqualTo(1);
        assertThatJson(result.getResponse().getContentAsString())
            .inPath("list.pagination.hasMoreItems")
            .isEqualTo(false);
        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.totalItems").isEqualTo(12);
    }

    @Test
    public void getVariablesShouldReturnAllResultsUsingHalWhenMediaTypeIsApplicationHalJson() throws Exception {
        //given
        PageRequest pageRequest = PageRequest.of(1, 10);
        TaskVariableEntity variableEntity = buildVariable();
        given(variableRepository.findAll(any(Predicate.class), eq(pageRequest)))
            .willReturn(new PageImpl<>(Collections.singletonList(variableEntity), pageRequest, 11));

        //when
        mockMvc
            .perform(
                get("/v1/tasks/{taskId}/variables?page=1&size=10", variableEntity.getTaskId())
                    .accept(MediaTypes.HAL_JSON_VALUE)
            )
            //then
            .andExpect(status().isOk());
    }

    private TaskVariableEntity buildVariable() {
        TaskVariableEntity variableEntity = new TaskVariableEntity(
            1L,
            String.class.getName(),
            "firstName",
            UUID.randomUUID().toString(),
            "My app",
            "My app",
            "1",
            null,
            null,
            UUID.randomUUID().toString(),
            new Date(),
            new Date(),
            UUID.randomUUID().toString()
        );
        variableEntity.setValue("John");
        return variableEntity;
    }
}
