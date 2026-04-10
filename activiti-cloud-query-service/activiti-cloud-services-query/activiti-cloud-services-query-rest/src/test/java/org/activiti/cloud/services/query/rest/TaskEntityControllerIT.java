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
import static org.activiti.cloud.services.query.rest.TestTaskEntityBuilder.buildDefaultTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.querydsl.core.types.Predicate;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.api.task.model.QueryCloudTask.TaskPermissions;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(TaskController.class)
@Import(
    {
        QueryRestWebMvcAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class,
        CommonExceptionHandlerQuery.class,
    }
)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
@TestPropertySource(
    locations = { "classpath:application-test.properties" },
    properties = "activiti.cloud.rest.max-items.enabled=true"
)
class TaskEntityControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private VariableRepository processVariableRepository;

    @MockitoBean
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @MockitoBean
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @MockitoBean
    private EntityFinder entityFinder;

    @MockitoBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockitoBean
    private ProcessDefinitionRepository processDefinitionRepository;

    @MockitoBean
    private SecurityPoliciesProperties securityPoliciesProperties;

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
    void findAllShouldReturnAllResultsUsingAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11, 10, PageRequest.of(0, 20));

        given(taskRepository.findAll(nullable(Predicate.class), eq(pageRequest)))
            .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultTask()), pageRequest, 12));

        //when
        MvcResult result = mockMvc
            .perform(get("/v1/tasks?skipCount=11&maxItems=10").accept(MediaType.APPLICATION_JSON))
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
    void findAllShouldReturnAllResultsUsingHalWhenMediaTypeIsApplicationHalJson() throws Exception {
        //given
        PageRequest pageRequest = PageRequest.of(1, 10);

        when(taskRepository.findAll(nullable(Predicate.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(buildDefaultTask()), pageRequest, 11));

        //when
        mockMvc
            .perform(get("/v1/tasks?page=1&size=10").accept(MediaTypes.HAL_JSON_VALUE))
            //then
            .andExpect(status().isOk());
    }

    @Test
    void findByIdShouldUseAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        TaskEntity taskEntity = buildDefaultTask();
        given(entityFinder.findById(eq(taskRepository), eq(taskEntity.getId()), anyString())).willReturn(taskEntity);

        Predicate restrictionPredicate = mock(Predicate.class);
        given(taskLookupRestrictionService.restrictToInvolvedUsersQuery(any())).willReturn(restrictionPredicate);
        given(taskRepository.existsInProcessInstanceScope(restrictionPredicate)).willReturn(true);
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");

        //when
        this.mockMvc.perform(get("/v1/tasks/{taskId}", taskEntity.getId()).accept(MediaType.APPLICATION_JSON_VALUE))
            //then
            .andExpect(status().isOk());
    }

    @Test
    void should_returnCandidates_when_invokeGetTaskById() throws Exception {
        //given
        TaskEntity taskEntity = buildDefaultTask();
        taskEntity.setTaskCandidateGroups(buildCandidateGroups(taskEntity));
        taskEntity.setTaskCandidateUsers(buildCandidateUsers(taskEntity));

        given(entityFinder.findById(eq(taskRepository), eq(taskEntity.getId()), anyString())).willReturn(taskEntity);

        Predicate restrictionPredicate = mock(Predicate.class);
        given(taskLookupRestrictionService.restrictToInvolvedUsersQuery(any())).willReturn(restrictionPredicate);
        given(taskRepository.existsInProcessInstanceScope(restrictionPredicate)).willReturn(true);
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");

        //when
        MvcResult mvcResult =
            this.mockMvc.perform(get("/v1/tasks/{taskId}", taskEntity.getId()).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        assertThatJson(mvcResult.getResponse().getContentAsString())
            .inPath("entry.candidateUsers")
            .isArray()
            .hasSize(1)
            .contains("testuser");

        assertThatJson(mvcResult.getResponse().getContentAsString())
            .inPath("entry.candidateGroups")
            .isArray()
            .hasSize(1)
            .contains("testgroup");
    }

    private Set<TaskCandidateGroupEntity> buildCandidateGroups(TaskEntity taskEntity) {
        TaskCandidateGroupEntity taskCandidateGroup = new TaskCandidateGroupEntity();
        taskCandidateGroup.setGroupId("testgroup");
        taskCandidateGroup.setTask(taskEntity);
        taskCandidateGroup.setTaskId(taskEntity.getId());
        Set<TaskCandidateGroupEntity> groups = new HashSet<>();
        groups.add(taskCandidateGroup);
        return groups;
    }

    private Set<TaskCandidateUserEntity> buildCandidateUsers(TaskEntity taskEntity) {
        TaskCandidateUserEntity taskCandidateUser = new TaskCandidateUserEntity();
        taskCandidateUser.setUserId("testuser");
        taskCandidateUser.setTask(taskEntity);
        taskCandidateUser.setTaskId(taskEntity.getId());
        Set<TaskCandidateUserEntity> users = new HashSet<>();
        users.add(taskCandidateUser);
        return users;
    }

    @Test
    void should_returnTaskPermissions_when_invokeGetTaskById() throws Exception {
        //given
        TaskEntity taskEntity = buildDefaultTask();
        taskEntity.setTaskCandidateUsers(buildCandidateUsers(taskEntity));
        given(entityFinder.findById(eq(taskRepository), eq(taskEntity.getId()), anyString())).willReturn(taskEntity);

        Predicate restrictionPredicate = mock(Predicate.class);
        given(taskLookupRestrictionService.restrictToInvolvedUsersQuery(any())).willReturn(restrictionPredicate);
        given(taskRepository.existsInProcessInstanceScope(restrictionPredicate)).willReturn(true);
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");

        //when
        MvcResult mvcResult =
            this.mockMvc.perform(get("/v1/tasks/{taskId}", taskEntity.getId()).accept(MediaType.APPLICATION_JSON_VALUE))
                //then
                .andExpect(status().isOk())
                .andReturn();
        assertThatJson(mvcResult.getResponse().getContentAsString())
            .inPath("entry.permissions")
            .isArray()
            .hasSize(1)
            .contains(TaskPermissions.VIEW);
    }

    @Test
    void should_returnBadRequest_when_invokeWithPagingParametersExceedingLimits() throws Exception {
        //given
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(1000, 1000, PageRequest.of(0, 1000));

        given(taskRepository.findAll(nullable(Predicate.class), eq(pageRequest)))
            .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultTask()), pageRequest, 2000));

        //when
        mockMvc
            .perform(get("/v1/tasks?skipCount=1000&maxItems=1001").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.entry.message").value("Exceeded max limit of 1000 elements"));
    }

    @Test
    void should_returnBadRequest_when_invokeWithPageParameterExceedingLimits() throws Exception {
        //given
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(1000, 1000, PageRequest.of(0, 1000));

        given(taskRepository.findAll(nullable(Predicate.class), eq(pageRequest)))
            .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultTask()), pageRequest, 2000));

        //when
        mockMvc
            .perform(get("/v1/tasks?page=0&size=1001").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.entry.message").value("Exceeded max limit of 1000 elements"));
    }

    @Test
    void should_returnOK_when_invokeWithPagingParametersWithinLimits() throws Exception {
        //given
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(0, 1000, PageRequest.of(0, 20));

        given(taskRepository.findAll(nullable(Predicate.class), eq(pageRequest)))
            .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultTask()), pageRequest, 1001));

        //when
        mockMvc
            .perform(get("/v1/tasks?skipCount=0&maxItems=1000").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void should_returnOK_when_invokeWithPageParameterWithinLimits() throws Exception {
        //given
        PageRequest pageRequest = PageRequest.of(0, 1000);

        given(taskRepository.findAll(nullable(Predicate.class), eq(pageRequest)))
            .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultTask()), pageRequest, 1001));

        //when
        mockMvc
            .perform(get("/v1/tasks?page=0&size=1000").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        //given
        String taskId = "nonexistent-task-id";
        given(entityFinder.findById(eq(taskRepository), eq(taskId), anyString()))
            .willThrow(new EntityNotFoundException("Unable to find taskEntity for the given id:'" + taskId + "'"));

        //when
        mockMvc
            .perform(get("/v1/tasks/{taskId}", taskId).accept(MediaType.APPLICATION_JSON))
            //then
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.message").value("Unable to find taskEntity for the given id:'" + taskId + "'"));
    }

    @Test
    void shouldReturnNotFoundForCandidateUsersWhenTaskDoesNotExist() throws Exception {
        //given
        String taskId = "nonexistent-task-id";
        given(entityFinder.findById(eq(taskRepository), eq(taskId), anyString()))
            .willThrow(new EntityNotFoundException("Unable to find taskEntity for the given id:'" + taskId + "'"));

        //when
        mockMvc
            .perform(get("/v1/tasks/{taskId}/candidate-users", taskId).accept(MediaType.APPLICATION_JSON))
            //then
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.message").value("Unable to find taskEntity for the given id:'" + taskId + "'"));
    }

    @Test
    void shouldReturnNotFoundForCandidateGroupsWhenTaskDoesNotExist() throws Exception {
        //given
        String taskId = "nonexistent-task-id";
        given(entityFinder.findById(eq(taskRepository), eq(taskId), anyString()))
            .willThrow(new EntityNotFoundException("Unable to find taskEntity for the given id:'" + taskId + "'"));

        //when
        mockMvc
            .perform(get("/v1/tasks/{taskId}/candidate-groups", taskId).accept(MediaType.APPLICATION_JSON))
            //then
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.message").value("Unable to find taskEntity for the given id:'" + taskId + "'"));
    }
}
