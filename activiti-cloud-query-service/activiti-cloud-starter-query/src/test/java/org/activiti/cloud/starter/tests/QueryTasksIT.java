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
package org.activiti.cloud.starter.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class QueryTasksIT {

    private static final String TASKS_URL = "/v1/tasks";
    private static final String ADMIN_TASKS_URL = "/admin/v1/tasks";

    private static final ParameterizedTypeReference<PagedModel<Task>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<Task>>() {};

    private static final ParameterizedTypeReference<Task> SINGLE_TASK_RESPONSE_TYPE = new ParameterizedTypeReference<Task>() {};

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

    @BeforeEach
    public void setUp() {
        eventsAggregator = new EventsAggregator(producer);
        processInstanceBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
        taskEventContainedBuilder = new TaskEventContainedBuilder(eventsAggregator);
        runningProcessInstance = processInstanceBuilder.aRunningProcessInstance("My running instance");
    }

    @AfterEach
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
        Task cancelledTask = taskEventContainedBuilder.aCancelledTask("Cancelled task",
                                                                      runningProcessInstance);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetTasks();

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
                                    Task.TaskStatus.COMPLETED),
                              tuple(cancelledTask.getId(),
                                    Task.TaskStatus.CANCELLED));
        });

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = testRestTemplate.exchange(TASKS_URL + "?status={status}",
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


            //when
            ResponseEntity<PagedModel<Task>> cancelEntity = testRestTemplate.exchange(TASKS_URL + "?status={status}",
                    HttpMethod.GET,
                    keycloakTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    Task.TaskStatus.CANCELLED);

            //then
            assertThat(cancelEntity).isNotNull();
            assertThat(cancelEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(cancelEntity.getBody()).isNotNull();
            Collection<Task> cancelledTasks = cancelEntity.getBody().getContent();
            assertThat(cancelledTasks)
                    .extracting(Task::getId,
                            Task::getStatus)
                    .containsExactly(tuple(cancelledTask.getId(),
                            Task.TaskStatus.CANCELLED));
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
        updatedTask.setProcessInstanceId(assignedTask.getProcessInstanceId());
        updatedTask.setName("Updated name");
        updatedTask.setDescription("Updated description");
        updatedTask.setPriority(42);
        updatedTask.setFormKey("FormKey");

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
            assertThat(task.getName()).isEqualTo(updatedTask.getName());
            assertThat(task.getDescription()).isEqualTo(updatedTask.getDescription());
            assertThat(task.getPriority()).isEqualTo(updatedTask.getPriority());
            assertThat(task.getFormKey()).isEqualTo(updatedTask.getFormKey());
        });
    }

    @Test
    public void shouldGetAvailableTasksAndFilterParentId() {
        //given
        Task createdTask = taskEventContainedBuilder.aCreatedStandaloneTaskWithParent("Created task with parent");

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetTasks();

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

    @Test
    public void shouldGetAvailableRootTasksWithStatus() {
        //given
        TaskImpl rootTaskNoSubtask = new TaskImpl(UUID.randomUUID().toString(),
                                         "Root task without subtask",
                                         Task.TaskStatus.ASSIGNED);
            rootTaskNoSubtask.setProcessInstanceId(runningProcessInstance.getId());
            rootTaskNoSubtask.setParentTaskId(null);
            eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(rootTaskNoSubtask));


        TaskImpl rootTask = aCreatedTask("Root task");
        rootTask.setProcessInstanceId(runningProcessInstance.getId());
        rootTask.setParentTaskId(null);
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(rootTask));

        TaskImpl task = aCreatedTask("Task with parent");
        task.setProcessInstanceId(runningProcessInstance.getId());
        task.setParentTaskId(rootTask.getId());
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {
            //when
            ResponseEntity<PagedModel<Task>> responseEntity = testRestTemplate.exchange(TASKS_URL + "?rootTasksOnly=true&status={status}",
                                                                                            HttpMethod.GET,
                                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                            PAGED_TASKS_RESPONSE_TYPE,
                                                                                            Task.TaskStatus.CREATED);
            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId)
                    .containsExactly(rootTask.getId());
        });
    }

    @Test
    public void shouldGetAvailableStandaloneTasksWithStatus() {
        //given
        TaskImpl processTask = new TaskImpl(UUID.randomUUID().toString(),
                                            "Task1",
                                            Task.TaskStatus.ASSIGNED);
        processTask.setProcessInstanceId(runningProcessInstance.getId());
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(processTask));


        TaskImpl standAloneTask = aCreatedTask("Task2");
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(standAloneTask));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {
            //when
            ResponseEntity<PagedModel<Task>> responseEntity = testRestTemplate.exchange(TASKS_URL + "?standalone=true&status={status}",
                                                                                            HttpMethod.GET,
                                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                            PAGED_TASKS_RESPONSE_TYPE,
                                                                                            Task.TaskStatus.CREATED);
            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId)
                    .containsExactly(standAloneTask.getId());
        });
    }

    private void checkExistingTask(Task createdTask) {
        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetTasks();

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
        keycloakTokenProducer.setKeycloakTestUser("testuser");
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



    @Test
    public void shouldGetAdminTask() {
        //given
              //given
        Task createdTask = taskEventContainedBuilder.aCreatedTask("Created task",
                                                                  runningProcessInstance);
        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            keycloakTokenProducer.setKeycloakTestUser("hradmin");
            //when
            ResponseEntity<Task> responseEntity = testRestTemplate.exchange(ADMIN_TASKS_URL + "/" + createdTask.getId(),
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         new ParameterizedTypeReference<Task>() {
                                         });

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getId()).isNotNull();
            assertThat(responseEntity.getBody().getId()).isEqualTo(createdTask.getId());

        });


    }

    private void assertCanRetrieveTask(Task task) {
       await().untilAsserted(() -> {

            ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetTasks();

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

    @Test
    public void shouldGetAddRemoveTaskUserCandidates() {
        //given
        Task createdTask = taskEventContainedBuilder.aTaskWithUserCandidate("task with user candidate",
                                                                            "testuser",
                                                                            runningProcessInstance);
        eventsAggregator.sendAll();

        keycloakTokenProducer.setKeycloakTestUser("testuser");

        //when
        ResponseEntity<List<String>> responseEntity = getCandidateUsers(createdTask.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().size()).isEqualTo(1);
        assertThat(responseEntity.getBody().get(0)).isEqualTo("testuser");

        //Check adding user candidate
        //when
        TaskCandidateUserImpl addCandidateUser = new TaskCandidateUserImpl("hruser",
                                                                       createdTask.getId());
        producer.send(new CloudTaskCandidateUserAddedEventImpl(addCandidateUser));

        //then
        responseEntity = getCandidateUsers(createdTask.getId());

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().size()).isEqualTo(2);
        assertThat(responseEntity.getBody().get(0)).isIn("hruser","testuser");
        assertThat(responseEntity.getBody().get(1)).isIn("hruser","testuser");

        //Check deleting user candidate
        //when
        TaskCandidateUserImpl deleteCandidateUser = new TaskCandidateUserImpl("hruser",
                                                                              createdTask.getId());
        producer.send(new CloudTaskCandidateUserRemovedEventImpl(deleteCandidateUser));

        //then
        responseEntity = getCandidateUsers(createdTask.getId());

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().size()).isEqualTo(1);
        assertThat(responseEntity.getBody().get(0)).isEqualTo("testuser");

    }

    @Test
    public void shouldGetAddRemoveTaskGroupCandidates() {
        //given
        Task createdTask = taskEventContainedBuilder.aTaskWithGroupCandidate("task with group candidate",
                                                                             "testgroup",
                                                                             runningProcessInstance);
        eventsAggregator.sendAll();

        keycloakTokenProducer.setKeycloakTestUser("testuser");

        //when
        ResponseEntity<List<String>> responseEntity = getCandidateGroups(createdTask.getId());
        ResponseEntity<Task> taskResponseEntity = executeRequestGetTasksById(createdTask.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().size()).isEqualTo(1);
        assertThat(responseEntity.getBody()).containsExactly("testgroup");
        assertThat(taskResponseEntity.getBody().getCandidateGroups()).containsExactly("testgroup");

        //Check adding group candidate
        //when
        TaskCandidateGroupImpl addCandidateGroup = new TaskCandidateGroupImpl("hrgroup",
                                                                              createdTask.getId());
        producer.send(new CloudTaskCandidateGroupAddedEventImpl(addCandidateGroup));

        //then
        responseEntity = getCandidateGroups(createdTask.getId());
        taskResponseEntity = executeRequestGetTasksById(createdTask.getId());

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().size()).isEqualTo(2);
        assertThat(responseEntity.getBody()).containsExactlyInAnyOrder("hrgroup","testgroup");
        assertThat(taskResponseEntity.getBody().getCandidateGroups()).containsExactlyInAnyOrder("hrgroup","testgroup");

        //Check deleting group candidate
        //when
        TaskCandidateGroupImpl deleteCandidateGroup = new TaskCandidateGroupImpl("hrgroup",
                                                                                 createdTask.getId());
        producer.send(new CloudTaskCandidateGroupRemovedEventImpl(deleteCandidateGroup));

        //then
        responseEntity = getCandidateGroups(createdTask.getId());
        taskResponseEntity = executeRequestGetTasksById(createdTask.getId());

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().size()).isEqualTo(1);
        assertThat(responseEntity.getBody()).containsExactly("testgroup");
        assertThat(taskResponseEntity.getBody().getCandidateGroups()).containsExactly("testgroup");
    }

    @Test
    public void adminShouldAssignTask() {
        //given
              //given
        Task createdTask = taskEventContainedBuilder.aCreatedTask("Created task",
                                                                  runningProcessInstance);
        eventsAggregator.sendAll();

        await().forever()
               .untilAsserted(() -> {

            keycloakTokenProducer.setKeycloakTestUser("hradmin");

            //when
            ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(ADMIN_TASKS_URL + "/" + createdTask.getId(),
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         new ParameterizedTypeReference<CloudTask>() {
                                         });

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getId()).isEqualTo(createdTask.getId());
            assertThat(responseEntity.getBody().getAssignee()).isNull();


            //when
            TaskImpl assignedTask = new TaskImpl(createdTask.getId(),
                                                 createdTask.getName(),
                                                 createdTask.getStatus());
            assignedTask.setAssignee("hruser");

            producer.send(new CloudTaskAssignedEventImpl(assignedTask));

            //then
            responseEntity = testRestTemplate.exchange(ADMIN_TASKS_URL + "/" + createdTask.getId(),
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         new ParameterizedTypeReference<CloudTask>() {
                                         });

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getId()).isNotNull();
            assertThat(responseEntity.getBody().getId()).isEqualTo(createdTask.getId());
            assertThat(responseEntity.getBody().getAssignee()).isEqualTo("hruser");

            //Restore user
            keycloakTokenProducer.setKeycloakTestUser("testuser");

        });
    }


    @Test
    public void shouldGetCorrectCompletedDateAndDurationWhenCompleted(){
        //given

        Date now = new Date(System.currentTimeMillis());
        Date yesterday = new Date(System.currentTimeMillis() - 86400000);

        Task completedTask = taskEventContainedBuilder.aCompletedTaskWithCreationDateAndCompletionDate("Completed task",
                runningProcessInstance,
                yesterday,
                now);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<Task> responseEntity = executeRequestGetTasksById(completedTask.getId());

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Task task = responseEntity.getBody();

            assertThat(task.getCompletedDate()).isNotNull();
            assertThat(task.getCompletedDate()).isEqualTo(now);

            assertThat(task.getDuration()).isEqualTo(now.getTime() - yesterday.getTime());
            assertThat(task.getDuration()).isNotNull();

        });

    }

    private void assertCannotSeeTask(Task task) {
        await().untilAsserted(() -> {

            ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetTasks();

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

    private ResponseEntity<PagedModel<Task>> executeRequestGetTasksFiltered(String name,String description) {
        String url=TASKS_URL;
        boolean add = false;
        if (name != null || description != null) {
            url += "?";
            if (name != null) {
                url += "name=" + name;
                add = true;
            }
            if (description != null) {
                if (add) {
                    url += "&";
                }
                url += "description=" + description;
            }

        }
        return testRestTemplate.exchange(url,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_TASKS_RESPONSE_TYPE);
    }

    private ResponseEntity<PagedModel<Task>> executeRequestGetTasks() {
        return testRestTemplate.exchange(TASKS_URL,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_TASKS_RESPONSE_TYPE);
    }

    private ResponseEntity<Task> executeRequestGetTasksById(String id) {
        return testRestTemplate.exchange(TASKS_URL + "/" + id,
                HttpMethod.GET,
                keycloakTokenProducer.entityWithAuthorizationHeader(),
                SINGLE_TASK_RESPONSE_TYPE);
    }

    private  ResponseEntity<List<String>> getCandidateUsers(String taskId) {
        return testRestTemplate.exchange(TASKS_URL + "/" + taskId+"/candidate-users",
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         new ParameterizedTypeReference<List<String>>() {
                                         });
    }

    private  ResponseEntity<List<String>> getCandidateGroups(String taskId) {
        return testRestTemplate.exchange(TASKS_URL + "/" + taskId+"/candidate-groups",
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         new ParameterizedTypeReference<List<String>>() {
                                         });
    }

    @Test
    public void shouldFilterTaskByCreatedDateFromTo(){
        //given
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date now = new Date();
        Date start1,complete1,start2,complete2,start3,complete3;


        complete1 = now;
        start1 = new Date(now.getTime() - 86400000);
        Task task1 = taskEventContainedBuilder.aCompletedTaskWithCreationDateAndCompletionDate("Completed task 1",
                runningProcessInstance,
                start1,
                complete1);

        complete2 = new Date(now.getTime() - 86400000);
        start2 = new Date(now.getTime() - (2*86400000));
        Task task2 = taskEventContainedBuilder.aCompletedTaskWithCreationDateAndCompletionDate("Completed task 2",
                runningProcessInstance,
                start2,
                complete2);

        complete3 = new Date(now.getTime() - (3*86400000));
        start3 = new Date(now.getTime() - (4*86400000));
        Task task3 = taskEventContainedBuilder.aCompletedTaskWithCreationDateAndCompletionDate("Completed task 3",
                runningProcessInstance,
                start3,
                complete3);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            //set check date 1 hour back from start2: we expect 2 tasks
            Date checkDate=new Date(start2.getTime() - 3600000);
            ResponseEntity<PagedModel<Task>> responseEntity = testRestTemplate.exchange(TASKS_URL+"?createdFrom="+sdf.format(checkDate),
                                                 HttpMethod.GET,
                                                 keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                 PAGED_TASKS_RESPONSE_TYPE
                                                 );
            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks.size()).isEqualTo(2);

            //when
            //set check date 1 hour after start2: we expect 1 task
            checkDate=new Date(start2.getTime() + 3600000);
            responseEntity = testRestTemplate.exchange(TASKS_URL+"?createdFrom="+sdf.format(checkDate),
                                                 HttpMethod.GET,
                                                 keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                 PAGED_TASKS_RESPONSE_TYPE
                                                 );
            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            tasks = responseEntity.getBody().getContent();
            assertThat(tasks.size()).isEqualTo(1);

            //when
            //set check date for createdTo 1 hour after start2: we expect 2 tasks
            checkDate=new Date(start2.getTime() + 3600000);
            responseEntity = testRestTemplate.exchange(TASKS_URL+"?createdTo="+sdf.format(checkDate),
                                                 HttpMethod.GET,
                                                 keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                 PAGED_TASKS_RESPONSE_TYPE
                                                 );
            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            tasks = responseEntity.getBody().getContent();
            assertThat(tasks.size()).isEqualTo(2);

            //when
            //set check date for createdFrom 1 hour before start2
            //set check date for createdTo 1 hour after start2
            //we expect 1 task
            checkDate=new Date(start2.getTime() - 3600000);
            Date checkDate1=new Date(start2.getTime() + 3600000);

            responseEntity = testRestTemplate.exchange(TASKS_URL+"?createdFrom="+sdf.format(checkDate)+"&createdTo="+sdf.format(checkDate1),
                                                 HttpMethod.GET,
                                                 keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                 PAGED_TASKS_RESPONSE_TYPE
                                                 );
            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            tasks = responseEntity.getBody().getContent();
            assertThat(tasks.size()).isEqualTo(1);

        });

    }

    @Test
    public void shouldHaveCreatedStatusAfterBeingReleased(){
        Task releasedTask = taskEventContainedBuilder.aReleasedTask("released-task");

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<Task> responseEntity = executeRequestGetTasksById(releasedTask.getId());

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Task task = responseEntity.getBody();

            assertThat(task.getName()).isEqualTo("released-task");
            assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);

        });

    }

    @Test
    public void shouldGetAvailableStandaloneTasksFilteredByNameDescription() {
      //given
        Task task1 = taskEventContainedBuilder.aCreatedTask("Task 1 for filter",
                                                            runningProcessInstance);
        Task task2 = taskEventContainedBuilder.aCreatedTask("Task 2 not filter",
                                                            null);

        Task task3 = taskEventContainedBuilder.aCreatedTask("Task 3 for filter standalone",
                                                            null);

        TaskImpl task4 = aCreatedTask("Task 4 for filter description");
        task4.setDescription("My task description");
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task4));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetTasksFiltered("for filter",null);

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> task = responseEntity.getBody().getContent();
            assertThat(task)
                    .extracting(Task::getId,
                                Task::getStatus)
                    .contains(tuple(task1.getId(),
                                    Task.TaskStatus.CREATED),
                              tuple(task3.getId(),
                                    Task.TaskStatus.CREATED),
                              tuple(task4.getId(),
                                    Task.TaskStatus.CREATED));
        });

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetTasksFiltered("for filter","task descr");

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> task = responseEntity.getBody().getContent();
            assertThat(task)
                    .extracting(Task::getId,
                                Task::getStatus)
                    .contains(tuple(task4.getId(),
                                    Task.TaskStatus.CREATED));
        });

    }

    @Test
    public void shouldSetProcessDefinitionVersionAndBusinessKeyOnTaskWhenThisInformationIsAvailableInTheEvent() {
      //given
        //event with process definition version set
        TaskImpl task1 = aCreatedTask("Task1");

        CloudTaskCreatedEventImpl task1Created = new CloudTaskCreatedEventImpl(task1);
        task1Created.setProcessDefinitionVersion(10);
        task1Created.setBusinessKey("businessKey");

        eventsAggregator.addEvents(task1Created);

        //event with process definition unset
        TaskImpl task2 = aCreatedTask("Task2");

        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task2));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetTasks();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId,
                                Task::getStatus,
                                Task::getProcessDefinitionVersion,
                                Task::getBusinessKey)
                    .contains(tuple(task1.getId(),
                                    Task.TaskStatus.CREATED,
                                    10,
                                    "businessKey"),
                              tuple(task2.getId(),
                                    Task.TaskStatus.CREATED,
                                    null,
                                    null));
        });

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = testRestTemplate.exchange(TASKS_URL + "?processDefinitionVersion={processDefinitionVersion}",
                                                                                            HttpMethod.GET,
                                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                            PAGED_TASKS_RESPONSE_TYPE,
                                                                                            10);

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId)
                    .containsExactly(task1.getId());
        });

    }

    @Test
    public void should_getTask_when_queryFilteredByTaskDefinitionKey() {
        //given
        CloudTaskCreatedEventImpl task1Created = buildTaskCreatedEvent("Task1",
                                                                       "taskDefinitionKey");
        CloudTaskCreatedEventImpl task2Created = buildTaskCreatedEvent("Task2",
                                                                       null);
        eventsAggregator.addEvents(task1Created);
        eventsAggregator.addEvents(task2Created);
        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetTasks();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId,
                                Task::getStatus,
                                Task::getTaskDefinitionKey)
                    .contains(tuple(task1Created.getEntity().getId(),
                                    Task.TaskStatus.CREATED,
                                    "taskDefinitionKey"),
                              tuple(task2Created.getEntity().getId(),
                                    Task.TaskStatus.CREATED,
                                    null));
        });

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<Task>> responseEntity = testRestTemplate.exchange(TASKS_URL + "?taskDefinitionKey={taskDefinitionKey}",
                                                                                            HttpMethod.GET,
                                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                            PAGED_TASKS_RESPONSE_TYPE,
                                                                                            "taskDefinitionKey");

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(responseEntity.getBody()).isNotNull();
            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId)
                    .containsExactly(task1Created.getEntity().getId());
        });

    }

    private CloudTaskCreatedEventImpl buildTaskCreatedEvent(String taskName,
                                                            String taskDefinitionKey) {
        TaskImpl task1 = aCreatedTask(taskName,
                                      taskDefinitionKey);

        return new CloudTaskCreatedEventImpl(task1);
    }

    private TaskImpl aCreatedTask(String taskName,
                                  String taskDefinitionKey) {
        TaskImpl task = aCreatedTask(taskName);
        task.setTaskDefinitionKey(taskDefinitionKey);
        return task;
    }

    private TaskImpl aCreatedTask(String taskName) {
        return new TaskImpl(UUID.randomUUID().toString(),
                            taskName,
                            Task.TaskStatus.CREATED);
    }

    @Test
    public void shouldGetTaskGroupCandidatesAfterTaskCompleted() {
        //given
        Task task = taskEventContainedBuilder.aTaskWithGroupCandidate("task with group candidate",
                                                                      "testgroup",
                                                                      runningProcessInstance);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {
            //when
            ResponseEntity<List<String>> response = getCandidateGroups(task.getId());
            ResponseEntity<Task> taskResponseEntity = executeRequestGetTasksById(task.getId());

            //then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody()).containsExactly("testgroup");
            assertThat(taskResponseEntity.getBody().getCandidateGroups()).containsExactly("testgroup");
        });

        keycloakTokenProducer.setKeycloakTestUser("testuser");

        ((TaskImpl)task).setAssignee("testuser");
        ((TaskImpl)task).setStatus(Task.TaskStatus.ASSIGNED);
        eventsAggregator.addEvents(new CloudTaskAssignedEventImpl(task));
        eventsAggregator.sendAll();

        await().untilAsserted(() -> {
            //when
            ResponseEntity<List<String>> response = getCandidateGroups(task.getId());
            ResponseEntity<Task> taskResponseEntity = executeRequestGetTasksById(task.getId());

            //then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody()).containsExactly("testgroup");
            assertThat(taskResponseEntity.getBody().getCandidateGroups()).containsExactly("testgroup");
        });

        ((TaskImpl)task).setStatus(Task.TaskStatus.COMPLETED);
        eventsAggregator.addEvents(new CloudTaskCompletedEventImpl(UUID.randomUUID().toString(), new Date().getTime(), task));
        eventsAggregator.sendAll();

        TaskCandidateGroupImpl candidateGroup = new TaskCandidateGroupImpl("hrgroup",
                                                                           task.getId());
        producer.send(new CloudTaskCandidateGroupRemovedEventImpl(candidateGroup));

        await().untilAsserted(() -> {
            //when
            ResponseEntity<List<String>> response = getCandidateGroups(task.getId());
            ResponseEntity<Task> taskResponseEntity = executeRequestGetTasksById(task.getId());

            //then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody()).containsExactly("testgroup");
            assertThat(taskResponseEntity.getBody().getCandidateGroups()).containsExactly("testgroup");
        });

    }

    @Test
    public void shouldGetTaskUserCandidatesAfterTaskCompleted() {
        //given
        Task task = taskEventContainedBuilder.aTaskWithUserCandidate("task with user candidate",
                                                                     "testuser",
                                                                     runningProcessInstance);
        eventsAggregator.sendAll();

        keycloakTokenProducer.setKeycloakTestUser("testuser");

        //when
        ResponseEntity<List<String>> responseEntity = getCandidateUsers(task.getId());
        ResponseEntity<Task> taskResponseEntity = executeRequestGetTasksById(task.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).hasSize(1);
        assertThat(responseEntity.getBody()).containsExactly("testuser");
        assertThat(taskResponseEntity.getBody().getCandidateUsers()).hasSize(1);
        assertThat(taskResponseEntity.getBody().getCandidateUsers()).containsExactly("testuser");

        //when
        ((TaskImpl)task).setAssignee("testuser");
        ((TaskImpl)task).setStatus(Task.TaskStatus.ASSIGNED);
        eventsAggregator.addEvents(new CloudTaskAssignedEventImpl(task));
        eventsAggregator.sendAll();

        responseEntity = getCandidateUsers(task.getId());
        taskResponseEntity = executeRequestGetTasksById(task.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).hasSize(1);
        assertThat(responseEntity.getBody()).containsExactly("testuser");
        assertThat(taskResponseEntity.getBody().getCandidateUsers()).hasSize(1);
        assertThat(taskResponseEntity.getBody().getCandidateUsers()).containsExactly("testuser");


        ((TaskImpl)task).setStatus(Task.TaskStatus.COMPLETED);
        eventsAggregator.addEvents(new CloudTaskCompletedEventImpl(UUID.randomUUID().toString(), new Date().getTime(), task));
        eventsAggregator.sendAll();

        TaskCandidateUserImpl candidateUser = new TaskCandidateUserImpl("testuser",
                                                                        task.getId());
        producer.send(new CloudTaskCandidateUserRemovedEventImpl(candidateUser));

        //then
        responseEntity = getCandidateUsers(task.getId());
        taskResponseEntity = executeRequestGetTasksById(task.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).hasSize(1);
        assertThat(responseEntity.getBody()).containsExactly("testuser");
        assertThat(taskResponseEntity.getBody().getCandidateUsers()).hasSize(1);
        assertThat(taskResponseEntity.getBody().getCandidateUsers()).containsExactly("testuser");

    }

}
