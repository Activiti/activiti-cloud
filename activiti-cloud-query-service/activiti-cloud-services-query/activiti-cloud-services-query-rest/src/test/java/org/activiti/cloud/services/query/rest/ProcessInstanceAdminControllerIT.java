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
package org.activiti.cloud.services.query.rest;

import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.buildProcessInstanceEntity;
import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.createProcessVariables;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.querydsl.core.types.Predicate;
import jakarta.persistence.EntityManagerFactory;
import java.util.*;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProcessInstanceAdminController.class)
@Import(
    { QueryRestWebMvcAutoConfiguration.class, CommonModelAutoConfiguration.class, AlfrescoWebAutoConfiguration.class }
)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
@TestPropertySource(
    locations = { "classpath:application-test.properties" },
    properties = "activiti.cloud.rest.max-items.enabled=true"
)
class ProcessInstanceAdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private EntityFinder entityFinder;

    @MockBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockBean
    private ProcessDefinitionRepository processDefinitionRepository;

    @MockBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private TaskControllerHelper taskControllerHelper;

    @MockBean
    private VariableRepository processVariableRepository;

    @MockBean
    private EntityManagerFactory entityManagerFactory;

    @MockBean
    private ProcessInstanceAdminService processInstanceAdminService;

    @BeforeEach
    void setUp() {
        assertThat(entityManagerFactory).isNotNull();
    }

    @Test
    void shouldReturnProcessInstancesWithoutVariableKeys() throws Exception {
        //given
        ProcessInstanceEntity parentProcessInstance = buildProcessInstanceEntity();

        Page<ProcessInstanceEntity> processInstancePage = new PageImpl<>(
            Collections.singletonList(parentProcessInstance),
            PageRequest.of(1, 10),
            1
        );
        given(processInstanceRepository.findAll(any(Predicate.class), any(Pageable.class)))
            .willReturn(processInstancePage);
        given(processInstanceRepository.mapSubprocesses(any(), any(Pageable.class))).willReturn(processInstancePage);
        //when
        mockMvc
            .perform(get("/admin/v1/process-instances?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON))
            //then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries[0].entry.id").value(parentProcessInstance.getId()))
            .andExpect(jsonPath("$.list.entries[0].entry.status").value(parentProcessInstance.getStatus().name()))
            .andExpect(
                jsonPath("$.list.entries[0].entry.processDefinitionId")
                    .value(parentProcessInstance.getProcessDefinitionId())
            );
    }

    @Test
    void shouldReturnProcessInstancesWithVariableKeys() throws Exception {
        //given
        ProcessInstanceEntity processInstanceEntity = buildProcessInstanceEntity();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity, 6);
        List<String> variableKeys = variables.stream().map(ProcessVariableEntity::getName).toList();

        Page<ProcessInstanceEntity> processInstancePage = new PageImpl<>(
            Collections.singletonList(processInstanceEntity),
            PageRequest.of(1, 10),
            1
        );
        given(processInstanceAdminService.findAllWithVariables(null, variableKeys, PageRequest.of(0, 10)))
            .willReturn(processInstancePage);
        given(processInstanceRepository.mapSubprocesses(any(), any(Pageable.class))).willReturn(processInstancePage);

        //when
        mockMvc
            .perform(
                get("/admin/v1/process-instances?variableKeys={variableKeys}&skipCount=10&maxItems=10", variableKeys)
                    .accept(MediaType.APPLICATION_JSON)
            )
            //then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries[0].entry.id").value(processInstanceEntity.getId()))
            .andExpect(jsonPath("$.list.entries[0].entry.status").value(processInstanceEntity.getStatus().name()))
            .andExpect(jsonPath("$.list.entries[0].entry.serviceName").value(processInstanceEntity.getServiceName()));
    }

    @Test
    void shouldReturnProcessInstanceById() throws Exception {
        //given
        ProcessInstanceEntity processInstanceEntity = buildProcessInstanceEntity();
        String processInstanceId = processInstanceEntity.getId();
        given(processInstanceAdminService.findById(processInstanceId)).willReturn(processInstanceEntity);
        given(processInstanceRepository.mapSubprocesses(processInstanceEntity)).willReturn(processInstanceEntity);

        //when
        mockMvc
            .perform(
                get("/admin/v1/process-instances/{processInstanceId}", processInstanceId)
                    .accept(MediaType.APPLICATION_JSON)
            )
            //then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entry.id").value(processInstanceEntity.getId()))
            .andExpect(jsonPath("$.entry.serviceName").value(processInstanceEntity.getServiceName()))
            .andExpect(jsonPath("$.entry.serviceFullName").value(processInstanceEntity.getServiceFullName()));
    }

    @Test
    void shouldReturnProcessAppVersions() throws Exception {
        //given
        given(processInstanceAdminService.findAllAppVersions(any(Predicate.class)))
            .willReturn(Collections.singleton("1.0"));

        //when
        mockMvc
            .perform(get("/admin/v1/process-instances/appVersions").accept(MediaType.APPLICATION_JSON))
            //then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("1.0"));
    }
}
