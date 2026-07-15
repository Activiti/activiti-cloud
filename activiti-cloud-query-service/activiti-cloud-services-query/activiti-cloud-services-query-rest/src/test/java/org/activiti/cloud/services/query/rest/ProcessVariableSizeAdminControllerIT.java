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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProcessVariableSizeAdminController.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import(
    { QueryRestWebMvcAutoConfiguration.class, CommonModelAutoConfiguration.class, AlfrescoWebAutoConfiguration.class }
)
@WithMockUser
public class ProcessVariableSizeAdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VariableRepository variableRepository;

    @MockitoBean
    private TaskVariableRepository taskVariableRepository;

    @MockitoBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @MockitoBean
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @MockitoBean
    private UserGroupManager userGroupManager;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockitoBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private ProcessInstanceAdminService processInstanceAdminService;

    @MockitoBean
    private ProcessInstanceService processInstanceService;

    @MockitoBean
    private ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

    @MockitoBean
    private EntityManagerFactory entityManagerFactory;

    @Test
    public void shouldReturnLargeVariablesWithDefaultMinSize() throws Exception {
        Object[] row = new Object[] { 1L, "largeVar", "string", "proc-1", "myProcess", 5000L };

        given(variableRepository.findLargeVariables(eq(4000), any(Pageable.class))).willReturn(
            new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1)
        );

        mockMvc
            .perform(get("/admin/v1/process-variables/size").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].name").value("largeVar"))
            .andExpect(jsonPath("$.content[0].type").value("string"))
            .andExpect(jsonPath("$.content[0].processInstanceId").value("proc-1"))
            .andExpect(jsonPath("$.content[0].processDefinitionKey").value("myProcess"))
            .andExpect(jsonPath("$.content[0].valueSize").value(5000));
    }

    @Test
    public void shouldReturnLargeVariablesWithCustomMinSize() throws Exception {
        Object[] row = new Object[] { 2L, "bigJson", "json", "proc-2", "orderProcess", 10000L };

        given(variableRepository.findLargeVariables(eq(8000), any(Pageable.class))).willReturn(
            new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1)
        );

        mockMvc
            .perform(
                get("/admin/v1/process-variables/size").param("minSize", "8000").accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(2))
            .andExpect(jsonPath("$.content[0].name").value("bigJson"))
            .andExpect(jsonPath("$.content[0].valueSize").value(10000));
    }

    @Test
    public void shouldReturnEmptyPageWhenNoLargeVariablesExist() throws Exception {
        given(variableRepository.findLargeVariables(eq(4000), any(Pageable.class))).willReturn(
            new PageImpl<>(List.of(), PageRequest.of(0, 20), 0)
        );

        mockMvc
            .perform(get("/admin/v1/process-variables/size").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0));
    }
}
