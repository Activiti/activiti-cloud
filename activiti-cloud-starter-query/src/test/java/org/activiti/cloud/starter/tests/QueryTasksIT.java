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

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.TaskEventContainedBuilder;
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
    private static final ParameterizedTypeReference<PagedResources<Task>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<Task>>() {
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
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

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
        taskCandidateGroupRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableTasksAndFilterOnStatus() {
        //given
        Task createdTask = taskEventContainedBuilder.aCreatedTask("Created task",
                                                                  runningProcessInstance);
        Task assignedTask = taskEventContainedBuilder.anAssignedTask("Assigned task",
                                                                     "testuser",
                                                                     runningProcessInstance);
        Task completedTask = taskEventContainedBuilder.aCompletedTask("Completed task",
                                                                      runningProcessInstance);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Task>> responseEntity = executeRequestGetTasks();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> task = responseEntity.getBody().getContent();
            assertThat(task)
                    .extracting(Task::getId,
                                Task::getStatus)
                    .contains(tuple(createdTask.getId(),
                                    Task.TaskStatus.CREATED),
                              tuple(assignedTask.getId(),
                                    Task.TaskStatus.ASSIGNED),
                              tuple(completedTask.getId(),
                                    Task.TaskStatus.COMPLETED));
        });

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Task>> responseEntity = testRestTemplate.exchange(TASKS_URL + "?status={status}",
                                                                                            HttpMethod.GET,
                                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                            PAGED_TASKS_RESPONSE_TYPE,
                                                                                            Task.TaskStatus.ASSIGNED);

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId,
                                Task::getStatus)
                    .containsExactly(tuple(assignedTask.getId(),
                                           Task.TaskStatus.ASSIGNED));
        });
    }

    @Test
    public void shouldGetTaskWithUpdatedInfo() {
        //given
        Task assignedTask = taskEventContainedBuilder.anAssignedTask("Assigned task",
                                                                     "testuser",
                                                                     runningProcessInstance);

        eventsAggregator.sendAll();

        TaskImpl updatedTask = new TaskImpl(assignedTask.getId(),
                                            assignedTask.getName(),
                                            assignedTask.getStatus());
        updatedTask.setAssignee(assignedTask.getAssignee());
        updatedTask.setProcessInstanceId(assignedTask.getProcessInstanceId());
        updatedTask.setDescription("Updated description");
        updatedTask.setPriority(42);

        //when
        producer.send(new CloudTaskUpdatedEventImpl(updatedTask));

        await().untilAsserted(() -> {

            ResponseEntity<Task> responseEntity = testRestTemplate.exchange(TASKS_URL + "/" + assignedTask.getId(),
                                                                            HttpMethod.GET,
                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                            new ParameterizedTypeReference<Task>() {
                                                                            });

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Task task = responseEntity.getBody();
            assertThat(task.getDescription()).isEqualTo("Updated description");
            assertThat(task.getPriority()).isEqualTo(42);
        });
    }

    @Test
    public void shouldGetAvailableTasksAndFilterParentId() {
        //given
        Task createdTask = taskEventContainedBuilder.aCreatedStandaloneTaskWithParent("Created task with parent");

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Task>> responseEntity = executeRequestGetTasks();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> task = responseEntity.getBody().getContent();
            assertThat(task)
                    .extracting(Task::getId,
                                Task::getStatus,
                                Task::getParentTaskId)
                    .contains(tuple(createdTask.getId(),
                                    Task.TaskStatus.CREATED,
                                    createdTask.getParentTaskId()));
        });
    }

    @Test
    public void shouldGetStandaloneAssignedTasksAndFilterParentId() {
        //given
        Task createdTask = taskEventContainedBuilder.aCreatedStandaloneAssignedTaskWithParent("Created task with parent",
                                                                                              "testuser");

        eventsAggregator.sendAll();

        checkExistingTask(createdTask);
    }

    @Test
    public void shouldGetAssignedTasksAndFilterParentId() {
        //given
        Task createdTask = taskEventContainedBuilder.anAssignedTaskWithParent("Created task with parent",
                                                                              "testuser",
                                                                              runningProcessInstance);

        eventsAggregator.sendAll();

        checkExistingTask(createdTask);
    }

    private void checkExistingTask(Task createdTask) {
        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Task>> responseEntity = executeRequestGetTasks();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> task = responseEntity.getBody().getContent();
            assertThat(task)
                    .extracting(Task::getId,
                                Task::getStatus,
                                Task::getParentTaskId)
                    .contains(tuple(createdTask.getId(),
                                    Task.TaskStatus.ASSIGNED,
                                    createdTask.getParentTaskId()));
        });
    }

    @Test
    public void shouldGetRestrictedTasksWithUserPermission() {
        //given
        Task taskWithCandidate = taskEventContainedBuilder.aTaskWithUserCandidate("task with candidate",
                                                                                  "testuser",
                                                                                  runningProcessInstance);
        //when
        eventsAggregator.sendAll();

        //then
        assertCanRetrieveTask(taskWithCandidate);
    }

    @Test
    public void shouldNotGetRestrictedTasksWithoutUserPermission() {
        //given
        Task taskWithCandidate = taskEventContainedBuilder.aTaskWithUserCandidate("task with candidate",
                                                                                  "specialUser",
                                                                                  runningProcessInstance);

        //when
        eventsAggregator.sendAll();

        //then
        assertCannotSeeTask(taskWithCandidate);
    }

    @Test
    public void shouldGetRestrictedTasksWithGroupPermission() {
        //given
        //we are logged in as testuser who belongs to testgroup, so it should be able to see the task
        Task taskWithCandidate = taskEventContainedBuilder.aTaskWithGroupCandidate("task with candidate",
                                                                                   "testgroup",
                                                                                   runningProcessInstance);

        //when
        eventsAggregator.sendAll();

        //then
        assertCanRetrieveTask(taskWithCandidate);
    }

    private void assertCanRetrieveTask(Task task) {
        await().untilAsserted(() -> {

            ResponseEntity<PagedResources<Task>> responseEntity = executeRequestGetTasks();

            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId,
                                Task::getStatus)
                    .contains(tuple(task.getId(),
                                    Task.TaskStatus.CREATED));
        });
    }

    @Test
    public void shouldNotGetRestrictedTasksWithoutGroupPermission() {
        //given
        //we are logged in as test user who does not belong to hrgroup, so it should not be available
        Task taskWithCandidate = taskEventContainedBuilder.aTaskWithGroupCandidate("task with candidate",
                                                                                   "hrgroup",
                                                                                   runningProcessInstance);
        //when
        eventsAggregator.sendAll();

        //then
        assertCannotSeeTask(taskWithCandidate);
    }

    private void assertCannotSeeTask(Task task) {
        await().untilAsserted(() -> {

            ResponseEntity<PagedResources<Task>> responseEntity = executeRequestGetTasks();

            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            //don't see the task as not for me
            assertThat(tasks)
                    .extracting(Task::getId)
                    .doesNotContain(task.getId());
        });
    }

    private ResponseEntity<PagedResources<Task>> executeRequestGetTasks() {
        return testRestTemplate.exchange(TASKS_URL,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_TASKS_RESPONSE_TYPE);
    }
}
