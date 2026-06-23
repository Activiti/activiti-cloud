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

import static org.activiti.cloud.services.query.rest.ProcessDefinitionBuilder.buildDefaultProcessDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import java.util.Collections;
import java.util.List;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
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

@WebMvcTest(ProcessDefinitionAdminController.class)
@Import(
    { QueryRestWebMvcAutoConfiguration.class, CommonModelAutoConfiguration.class, AlfrescoWebAutoConfiguration.class }
)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
public class ProcessDefinitionAdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessDefinitionRepository processDefinitionRepository;

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
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockitoBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private VariableRepository processVariableRepository;

    @MockitoBean
    private ProcessInstanceAdminService processInstanceAdminService;

    @MockitoBean
    private ProcessInstanceService processInstanceService;

    @MockitoBean
    private ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

    @MockitoBean
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        assertThat(processInstanceAdminService).isNotNull();
        assertThat(processInstanceService).isNotNull();
        assertThat(entityManagerFactory).isNotNull();
    }

    @Test
    public void shouldReturnAvailableProcessDefinitions() throws Exception {
        //given
        PageRequest pageRequest = PageRequest.of(0, 10);
        given(processDefinitionRepository.findAll(any(), eq(pageRequest))).willReturn(
            new PageImpl<>(Collections.singletonList(buildDefaultProcessDefinition()), pageRequest, 1)
        );

        //when
        mockMvc
            .perform(get("/admin/v1/process-definitions?page=0&size=10").accept(MediaTypes.HAL_JSON_VALUE))
            //then
            .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnAvailableProcessDefinitionsUsingAlfrescoFormat() throws Exception {
        //given
        given(processDefinitionRepository.findAll(any(), any(Pageable.class))).willReturn(
            new PageImpl<>(Collections.singletonList(buildDefaultProcessDefinition()), PageRequest.of(1, 10), 11)
        );

        //when
        mockMvc
            .perform(get("/admin/v1/process-definitions?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON))
            //then
            .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnLatestProcessDefinition() throws Exception {
        //given
        given(processDefinitionRepository.findAllLatestVersions(any()))
            .willReturn(Collections.singletonList(buildDefaultProcessDefinition()));
        //when
        mockMvc
            .perform(get("/admin/v1/process-definitions?latestVersion=true").accept(MediaTypes.HAL_JSON_VALUE))
            //then
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnDeduplicatedListWhenLatestVersionIsTrue() throws Exception {
        //given
        ProcessDefinitionEntity processOne = buildDefaultProcessDefinition();
        processOne.setKey("processOne");
        processOne.setVersion(3);
        ProcessDefinitionEntity processTwo = buildDefaultProcessDefinition();
        processTwo.setKey("processTwo");
        processTwo.setVersion(7);
        given(processDefinitionRepository.findAllLatestVersions(any()))
            .willReturn(List.of(processOne, processTwo));

        //when
        mockMvc
            .perform(get("/admin/v1/process-definitions?latestVersion=true").accept(MediaTypes.HAL_JSON_VALUE))
            //then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.processDefinitions", hasSize(2)));
    }

    @Test
    void shouldIgnorePaginationWhenLatestVersionIsTrue() throws Exception {
        //given
        given(processDefinitionRepository.findAllLatestVersions(any()))
            .willReturn(Collections.singletonList(buildDefaultProcessDefinition()));

        //when
        mockMvc
            .perform(
                get("/admin/v1/process-definitions?latestVersion=true&skipCount=10&maxItems=10").accept(
                    MediaTypes.HAL_JSON_VALUE
                )
            )
            //then
            .andExpect(status().isOk());
    }
}
