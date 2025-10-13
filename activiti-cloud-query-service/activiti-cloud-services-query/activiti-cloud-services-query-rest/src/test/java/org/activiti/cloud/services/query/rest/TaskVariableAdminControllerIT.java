/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
import org.activiti.QueryRestTestApplication;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.*;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(TaskVariableAdminController.class)
@Import({ QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class, QueryRestWebMvcAutoConfiguration.class })
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
public class TaskVariableAdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskVariableRepository variableRepository;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockitoBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockitoBean
    private EntityManagerFactory entityManagerFactory;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @MockitoBean
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @MockitoBean
    private VariableRepository processVariableRepository;

    @MockitoBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private ProcessInstanceAdminService processInstanceAdminService;

    @MockitoBean
    private ProcessInstanceService processInstanceService;

    @Test
    public void getVariablesShouldReturnAllResultsWithEphemeralFieldUsingAlfrescoMetadataWhenMediaTypeIsApplicationJson()
        throws Exception {
        //given
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11, 10, PageRequest.of(0, 20));

        TaskVariableEntity variableEntity = buildEphemeralVariable(true);

        given(variableRepository.findAll(any(Predicate.class), eq(pageRequest)))
            .willReturn(new PageImpl<>(Collections.singletonList(variableEntity)));

        //when
        MvcResult result = mockMvc
            .perform(
                get("/admin/v1/tasks/{taskId}/variables?skipCount=11&maxItems=10", variableEntity.getTaskId())
                    .accept(MediaType.APPLICATION_JSON)
            )
            //then
            .andExpect(status().isOk())
            .andReturn();

        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.skipCount").isEqualTo(11);
        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.count").isEqualTo(1);
        assertThatJson(result.getResponse().getContentAsString())
            .inPath("list.pagination.hasMoreItems")
            .isEqualTo(false);
        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.totalItems").isEqualTo(1);
        assertThatJson(result.getResponse().getContentAsString())
            .inPath("list.entries[0].entry.ephemeral")
            .isEqualTo(true);
    }

    @Test
    public void getVariablesShouldReturnAllResultsWithEphemeralFieldHasFalseValueUsingAlfrescoMetadataWhenMediaTypeIsApplicationJson()
        throws Exception {
        //given
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11, 10, PageRequest.of(0, 20));

        TaskVariableEntity variableEntity = buildEphemeralVariable(false);

        given(variableRepository.findAll(any(Predicate.class), eq(pageRequest)))
            .willReturn(new PageImpl<>(Collections.singletonList(variableEntity)));

        //when
        MvcResult result = mockMvc
            .perform(
                get("/admin/v1/tasks/{taskId}/variables?skipCount=11&maxItems=10", variableEntity.getTaskId())
                    .accept(MediaType.APPLICATION_JSON)
            )
            //then
            .andExpect(status().isOk())
            .andReturn();

        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.skipCount").isEqualTo(11);
        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.count").isEqualTo(1);
        assertThatJson(result.getResponse().getContentAsString())
            .inPath("list.pagination.hasMoreItems")
            .isEqualTo(false);
        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.totalItems").isEqualTo(1);
        assertThatJson(result.getResponse().getContentAsString())
            .inPath("list.entries[0].entry.ephemeral")
            .isEqualTo(false);
    }

    private TaskVariableEntity buildEphemeralVariable(boolean ephemeral) {
        TaskVariableEntity taskVariableEntity = new TaskVariableEntity(
            1L,
            "varName",
            "varValue",
            "processInstanceId",
            "serviceName",
            "serviceFullName",
            "serviceVersion",
            "appName",
            "appVersion",
            "taskId",
            new Date(),
            new Date(),
            UUID.randomUUID().toString(),
            ephemeral
        );
        taskVariableEntity.setValue("anyValue");
        return taskVariableEntity;
    }
}
