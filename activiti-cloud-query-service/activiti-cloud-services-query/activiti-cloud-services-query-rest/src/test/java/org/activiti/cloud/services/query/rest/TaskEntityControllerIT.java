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

package org.activiti.cloud.services.query.rest;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.cloud.services.query.rest.TestTaskEntityBuilder.buildDefaultTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.querydsl.core.types.Predicate;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskCandidateGroup;
import org.activiti.cloud.services.query.model.TaskCandidateUser;
import org.activiti.cloud.services.query.model.TaskEntity;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(TaskController.class)
@Import({
        QueryRestWebMvcAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class
})
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
public class TaskEntityControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private EntityFinder entityFinder;

    @MockBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockBean
    private ProcessDefinitionRepository processDefinitionRepository;

    @MockBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @BeforeEach
    public void setUp() {
        assertThat(securityManager).isNotNull();
        assertThat(securityPoliciesManager).isNotNull();
        assertThat(processDefinitionRepository).isNotNull();
        assertThat(securityPoliciesProperties).isNotNull();
        assertThat(taskLookupRestrictionService).isNotNull();
    }

    @Test
    public void findAllShouldReturnAllResultsUsingAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11,
                                                                  10,
                                                                  PageRequest.of(0,
                                                                                 20));

        given(taskRepository.findAll(any(),
                                     eq(pageRequest)))
                .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultTask()),
                                           pageRequest,
                                           12));

        //when
        MvcResult result = mockMvc.perform(get("/v1/tasks?skipCount=11&maxItems=10")
                                                   .accept(MediaType.APPLICATION_JSON))
                                   //then
                                   .andExpect(status().isOk())
                                   .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
                .node("list.pagination.skipCount").isEqualTo(11)
                .node("list.pagination.maxItems").isEqualTo(10)
                .node("list.pagination.count").isEqualTo(1)
                .node("list.pagination.hasMoreItems").isEqualTo(false)
                .node("list.pagination.totalItems").isEqualTo(12);
    }

    @Test
    public void findAllShouldReturnAllResultsUsingHalWhenMediaTypeIsApplicationHalJson() throws Exception {
        //given
        PageRequest pageRequest = PageRequest.of(1,
                                                 10);

        given(taskRepository.findAll(any(),
                                     eq(pageRequest)))
                .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultTask()),
                                           pageRequest,
                                           11));

        //when
        mockMvc.perform(get("/v1/tasks?page=1&size=10")
                                .accept(MediaTypes.HAL_JSON_VALUE))
                //then
                .andExpect(status().isOk());
    }

    @Test
    public void findByIdShouldUseAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        TaskEntity taskEntity = buildDefaultTask();
        given(entityFinder.findById(eq(taskRepository),
                                    eq(taskEntity.getId()),
                                    anyString()))
                .willReturn(taskEntity);

        Predicate restrictionPredicate = mock(Predicate.class);
        given(taskLookupRestrictionService.restrictTaskQuery(any())).willReturn(restrictionPredicate);
        given(taskRepository.findAll(restrictionPredicate)).willReturn(Collections.singletonList(taskEntity));

        //when
        this.mockMvc.perform(get("/v1/tasks/{taskId}",
                                 taskEntity.getId()).accept(MediaType.APPLICATION_JSON_VALUE))
                //then
                .andExpect(status().isOk());
    }

    @Test
    public void should_returnCandidates_when_invokeGetTaskById() throws Exception {
        //given
        TaskEntity taskEntity = buildDefaultTask();
        taskEntity.setTaskCandidateGroups(buildCandidateGroups(taskEntity));
        taskEntity.setTaskCandidateUsers(buildCandidateUsers(taskEntity));

        given(entityFinder.findById(eq(taskRepository),
                                    eq(taskEntity.getId()),
                                    anyString()))
                .willReturn(taskEntity);

        Predicate restrictionPredicate = mock(Predicate.class);
        given(taskLookupRestrictionService.restrictTaskQuery(any())).willReturn(restrictionPredicate);
        given(taskRepository.findAll(restrictionPredicate)).willReturn(Collections.singletonList(taskEntity));

        //when
        MvcResult mvcResult = this.mockMvc.perform(get("/v1/tasks/{taskId}",
                                                       taskEntity.getId()).accept(MediaType.APPLICATION_JSON_VALUE))
                                      .andExpect(status().isOk())
                                      .andReturn();

        assertThatJson(mvcResult.getResponse().getContentAsString())
                .node("entry.candidateUsers").isArray().ofLength(1).thatContains("testuser");

        assertThatJson(mvcResult.getResponse().getContentAsString())
                .node("entry.candidateGroups").isArray().ofLength(1).thatContains("testgroup");
    }

    private Set<TaskCandidateGroup> buildCandidateGroups(TaskEntity taskEntity) {
        TaskCandidateGroup taskCandidateGroup = new TaskCandidateGroup();
        taskCandidateGroup.setGroupId("testgroup");
        taskCandidateGroup.setTask(taskEntity);
        taskCandidateGroup.setTaskId(taskEntity.getId());
        Set<TaskCandidateGroup> groups = new HashSet<>();
        groups.add(taskCandidateGroup);
        return groups;
    }

    private Set<TaskCandidateUser> buildCandidateUsers(TaskEntity taskEntity) {
        TaskCandidateUser taskCandidateUser = new TaskCandidateUser();
        taskCandidateUser.setUserId("testuser");
        taskCandidateUser.setTask(taskEntity);
        taskCandidateUser.setTaskId(taskEntity.getId());
        Set<TaskCandidateUser> users = new HashSet<>();
        users.add(taskCandidateUser);
        return users;
    }
}
