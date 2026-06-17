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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.IntegrationContextRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IntegrationContextAdminController.class)
@Import(
    { QueryRestWebMvcAutoConfiguration.class, CommonModelAutoConfiguration.class, AlfrescoWebAutoConfiguration.class }
)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
@TestPropertySource(locations = "classpath:application-test.properties")
class IntegrationContextAdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntegrationContextRepository integrationContextRepository;

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

    @MockitoBean
    private EntityFinder entityFinder;

    @Test
    void shouldReturnNotFoundWhenIntegrationContextDoesNotExist() throws Exception {
        //given
        String integrationContextId = "nonexistent-id";
        given(integrationContextRepository.findById(integrationContextId)).willReturn(Optional.empty());

        //when
        mockMvc
            .perform(
                get("/admin/v1/integration-contexts/{integrationContextId}", integrationContextId)
                    .accept(MediaType.APPLICATION_JSON)
            )
            //then
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.entry.code").value(404))
            .andExpect(
                jsonPath("$.entry.message")
                    .value("Unable to find integration context for the given id: '" + integrationContextId + "'")
            );
    }
}
