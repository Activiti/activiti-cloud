/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.starter.tests;

import java.util.Collection;

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.TaskEventContainedBuilder;
import org.activiti.runtime.api.model.ProcessInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
public class QueryTasksIT {

    private static final String TASKS_URL = "/v1/tasks";
    private static final ParameterizedTypeReference<PagedResources<TaskEntity>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<TaskEntity>>() {
    };

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private MyProducer producer;

    private EventsAggregator eventsAggregator;

    private ProcessInstanceEventContainedBuilder processInstanceBuilder;

    private ProcessInstance runningProcessInstance;

    private TaskEventContainedBuilder taskEventContainedBuilder;

    @Before
    public void setUp() {
        eventsAggregator = new EventsAggregator(producer);
        processInstanceBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
        taskEventContainedBuilder = new TaskEventContainedBuilder(eventsAggregator);
        runningProcessInstance = processInstanceBuilder.aRunningProcessInstance("My running instance");
    }

    @After
    public void tearDown() {
        taskCandidateUserRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableTasksAndFilterOnStatus() {
        //given
        org.activiti.runtime.api.model.Task createdTask = taskEventContainedBuilder.aCreatedTask("Created task",
                                                                                                 runningProcessInstance);
        org.activiti.runtime.api.model.Task assignedTask = taskEventContainedBuilder.anAssignedTask("Assigned task",
                                                                                                    "jack",
                                                                                                    runningProcessInstance);
        org.activiti.runtime.api.model.Task completedTask = taskEventContainedBuilder.aCompletedTask("Completed task",
                                                                                                     runningProcessInstance);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<TaskEntity>> responseEntity = executeRequestGetTasks();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<TaskEntity> taskEntities = responseEntity.getBody().getContent();
            assertThat(taskEntities)
                    .extracting(TaskEntity::getId,
                                TaskEntity::getStatus)
                    .contains(tuple(createdTask.getId(),
                                    org.activiti.runtime.api.model.Task.TaskStatus.CREATED.name()),
                              tuple(assignedTask.getId(),
                                    org.activiti.runtime.api.model.Task.TaskStatus.ASSIGNED.name()),
                              tuple(completedTask.getId(),
                                    org.activiti.runtime.api.model.Task.TaskStatus.COMPLETED.name()));
        });

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<TaskEntity>> responseEntity = testRestTemplate.exchange(TASKS_URL + "?status={status}",
                                                                                                  HttpMethod.GET,
                                                                                                  keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                  PAGED_TASKS_RESPONSE_TYPE,
                                                                                                  org.activiti.runtime.api.model.Task.TaskStatus.ASSIGNED.name());

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<TaskEntity> taskEntities = responseEntity.getBody().getContent();
            assertThat(taskEntities)
                    .extracting(TaskEntity::getId,
                                TaskEntity::getStatus)
                    .containsExactly(tuple(assignedTask.getId(),
                                           org.activiti.runtime.api.model.Task.TaskStatus.ASSIGNED.name()));
        });
    }

    @Test
    public void shouldGetRestrictedTasksWithPermission() {
        //given
        org.activiti.runtime.api.model.Task taskWithCandidate = taskEventContainedBuilder.aTaskWithUserCandidate("task with candidate",
                                                                                                                 "testuser",
                                                                                                                 runningProcessInstance);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<TaskEntity>> responseEntity = executeRequestGetTasks();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<TaskEntity> taskEntities = responseEntity.getBody().getContent();
            assertThat(taskEntities)
                    .extracting(TaskEntity::getId,
                                TaskEntity::getStatus)
                    .contains(tuple(taskWithCandidate.getId(),
                                    org.activiti.runtime.api.model.Task.TaskStatus.CREATED.name()));
        });
    }

    @Test
    public void shouldNotGetRestrictedTasksWithoutPermission() throws Exception {
        //given
        org.activiti.runtime.api.model.Task taskWithCandidate = taskEventContainedBuilder.aTaskWithUserCandidate("task with candidate",
                                                                                                                 "specialUser",
                                                                                                                 runningProcessInstance);

        eventsAggregator.sendAll();

        Thread.sleep(300);

        //when
        ResponseEntity<PagedResources<TaskEntity>> responseEntity = executeRequestGetTasks();

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        Collection<TaskEntity> taskEntities = responseEntity.getBody().getContent();
        //don't see the task as not for me
        assertThat(taskEntities)
                .extracting(TaskEntity::getId)
                .doesNotContain(taskWithCandidate.getId());
    }

    private ResponseEntity<PagedResources<TaskEntity>> executeRequestGetTasks() {
        return testRestTemplate.exchange(TASKS_URL,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_TASKS_RESPONSE_TYPE);
    }
}
