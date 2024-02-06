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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.Task.TaskStatus;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.QueryCloudTask;
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
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.TaskEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.VariableEventContainedBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.CollectionUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
public class QueryTasksIT {

    private static final String TASKS_URL = "/v1/tasks";
    private static final String ADMIN_TASKS_URL = "/admin/v1/tasks";
    private static final String HRUSER = "hruser";
    private static final String TESTUSER = "testuser";

    private static final ParameterizedTypeReference<PagedModel<QueryCloudTask>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<QueryCloudTask>>() {};

    private static final ParameterizedTypeReference<PagedModel<Task>> PAGED_TASK_INTERFACE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<Task>>() {};

    private static final ParameterizedTypeReference<Task> SINGLE_TASK_RESPONSE_TYPE = new ParameterizedTypeReference<Task>() {};

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

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
    private TaskVariableRepository taskVariableRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private MyProducer producer;

    @Autowired
    private ProcessVariablesMigrationHelper processVariablesMigrationHelper;

    private EventsAggregator eventsAggregator;

    private ProcessInstanceEventContainedBuilder processInstanceBuilder;

    private ProcessInstance runningProcessInstance;

    private TaskEventContainedBuilder taskEventContainedBuilder;

    private VariableEventContainedBuilder variableEventContainedBuilder;

    @BeforeEach
    public void setUp() {
        eventsAggregator = new EventsAggregator(producer);
        processInstanceBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
        taskEventContainedBuilder = new TaskEventContainedBuilder(eventsAggregator);
        variableEventContainedBuilder = new VariableEventContainedBuilder(eventsAggregator);
        runningProcessInstance =
            processInstanceBuilder.aRunningProcessInstanceWithInitiator("ProcessInstanceWithInitiator", TESTUSER);
        identityTokenProducer.withTestUser(TESTUSER);
    }

    @AfterEach
    public void tearDown() {
        taskCandidateUserRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskVariableRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableTasksAndFilterOnStatus() {
        //given
        Task createdTask = taskEventContainedBuilder.aCreatedTask("Created task", runningProcessInstance);
        Task assignedTask = taskEventContainedBuilder.anAssignedTask(
            "Assigned task",
            "testuser",
            runningProcessInstance
        );
        Task completedTask = taskEventContainedBuilder.aCompletedTask("Completed task", runningProcessInstance);
        Task cancelledTask = taskEventContainedBuilder.aCancelledTask("Cancelled task", runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks();

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> task = responseEntity.getBody().getContent();
                assertThat(task)
                    .extracting(Task::getId, Task::getStatus)
                    .contains(
                        tuple(createdTask.getId(), Task.TaskStatus.CREATED),
                        tuple(assignedTask.getId(), Task.TaskStatus.ASSIGNED),
                        tuple(completedTask.getId(), Task.TaskStatus.COMPLETED),
                        tuple(cancelledTask.getId(), Task.TaskStatus.CANCELLED)
                    );
            });

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?status={status}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    Task.TaskStatus.ASSIGNED
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks)
                    .extracting(Task::getId, Task::getStatus)
                    .containsExactly(tuple(assignedTask.getId(), Task.TaskStatus.ASSIGNED));

                //when
                ResponseEntity<PagedModel<QueryCloudTask>> cancelEntity = testRestTemplate.exchange(
                    TASKS_URL + "?status={status}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    Task.TaskStatus.CANCELLED
                );

                //then
                assertThat(cancelEntity).isNotNull();
                assertThat(cancelEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(cancelEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> cancelledTasks = cancelEntity.getBody().getContent();
                assertThat(cancelledTasks)
                    .extracting(Task::getId, Task::getStatus)
                    .containsExactly(tuple(cancelledTask.getId(), Task.TaskStatus.CANCELLED));
            });
    }

    @Test
    public void should_getTasksFilteredOnVariableNameAndValue() {
        //given
        taskEventContainedBuilder.aCompletedTask("Task with no var", runningProcessInstance);
        Task taskApproved = taskEventContainedBuilder.aCompletedTask("Task approved", runningProcessInstance);
        Task taskRejected = taskEventContainedBuilder.aCompletedTask("Task rejected", runningProcessInstance);
        Task createdTask = taskEventContainedBuilder.aCreatedTask("Task Created", runningProcessInstance);

        variableEventContainedBuilder.aCreatedVariable("outcome", "approved").onTask(taskApproved);
        variableEventContainedBuilder.aCreatedVariable("intValue", 40).onTask(taskApproved);
        variableEventContainedBuilder.aCreatedVariable("outcome", "approved").onTask(createdTask);
        variableEventContainedBuilder.aCreatedVariable("outcome", "rejected").onTask(taskRejected);
        variableEventContainedBuilder.aCreatedVariable("anotherVariable", "approved").onTask(taskRejected);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> approvedTasks = executeGetTasksWithVariable(
                    "outcome",
                    "approved",
                    TaskStatus.COMPLETED
                );

                //then
                assertThat(approvedTasks).extracting(Task::getName).containsExactly(taskApproved.getName());
            });
    }

    @Test
    public void should_getTasksFilteredOnVariableNameAndValueWithPagedRequest() {
        //given
        taskEventContainedBuilder.aCompletedTask("Task with no var", runningProcessInstance);
        Task task1 = taskEventContainedBuilder.aCompletedTask("Task1", runningProcessInstance);
        Task task2 = taskEventContainedBuilder.aCompletedTask("Task2", runningProcessInstance);
        Task task3 = taskEventContainedBuilder.aCompletedTask("Task3", runningProcessInstance);
        Task task4 = taskEventContainedBuilder.aCompletedTask("Task4", runningProcessInstance);

        variableEventContainedBuilder.aCreatedVariable("outcome", "approved").onTask(task1);
        variableEventContainedBuilder.aCreatedVariable("intValue", 40).onTask(task1);
        variableEventContainedBuilder.aCreatedVariable("outcome", "approved").onTask(task3);
        variableEventContainedBuilder.aCreatedVariable("outcome", "approved").onTask(task2);
        variableEventContainedBuilder.aCreatedVariable("outcome", "rejected").onTask(task4);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                PagedModel<QueryCloudTask> approvedTasks = executeGetTasksWithVariablePagedRequest(
                    TASKS_URL,
                    "outcome",
                    "approved",
                    PageRequest.of(0, 2).withSort(Sort.Direction.DESC, "name")
                );

                //then
                assertThat(approvedTasks.getContent())
                    .hasSize(2)
                    .extracting(Task::getName)
                    .containsExactly(task3.getName(), task2.getName());
                assertThat(approvedTasks.getMetadata())
                    .extracting(
                        PagedModel.PageMetadata::getTotalElements,
                        PagedModel.PageMetadata::getNumber,
                        PagedModel.PageMetadata::getSize
                    )
                    .containsExactly(3L, 0L, 2L);
            });
    }

    @Test
    public void should_getTasksFilteredOnIntegerVariables() {
        //given
        taskEventContainedBuilder.aCompletedTask("Task with no var", runningProcessInstance);
        Task taskForties = taskEventContainedBuilder.aCompletedTask("Task forties", runningProcessInstance);
        Task taskThirties = taskEventContainedBuilder.aCompletedTask("Task thirties", runningProcessInstance);

        variableEventContainedBuilder.aCreatedVariable("intValue", 40).onTask(taskForties);
        variableEventContainedBuilder.aCreatedVariable("intValue", 30).onTask(taskThirties);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeGetTasksWithVariable(
                    "intValue",
                    30,
                    TaskStatus.COMPLETED
                );

                //then
                assertThat(retrievedTasks).extracting(Task::getName).containsExactly(taskThirties.getName());
            });
    }

    @Test
    public void should_getTasksFilteredOnBooleanVariables() {
        //given
        taskEventContainedBuilder.aCompletedTask("Task with no var", runningProcessInstance);
        Task taskApproved = taskEventContainedBuilder.aCompletedTask("Task approved", runningProcessInstance);
        Task taskRejected = taskEventContainedBuilder.aCompletedTask("Task rejected", runningProcessInstance);

        variableEventContainedBuilder.aCreatedVariable("approved", true).onTask(taskApproved);
        variableEventContainedBuilder.aCreatedVariable("approved", false).onTask(taskRejected);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeGetTasksWithVariable(
                    "approved",
                    true,
                    TaskStatus.COMPLETED
                );

                //then
                assertThat(retrievedTasks).extracting(Task::getName).containsExactly(taskApproved.getName());
            });
    }

    @Test
    public void should_getTasksFilteredOnBigDecimalVariables() {
        //given
        taskEventContainedBuilder.aCompletedTask("Task with no var", runningProcessInstance);
        Task taskScale2 = taskEventContainedBuilder.aCompletedTask("Task scale 2", runningProcessInstance);
        Task taskScale3 = taskEventContainedBuilder.aCompletedTask("Task scale 3", runningProcessInstance);

        variableEventContainedBuilder.aCreatedVariable("bigDecimalVar", BigDecimal.valueOf(100, 2)).onTask(taskScale2);
        BigDecimal bigDecimalScale3 = BigDecimal.valueOf(1000, 3);
        variableEventContainedBuilder.aCreatedVariable("bigDecimalVar", bigDecimalScale3).onTask(taskScale3);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeGetTasksWithVariable(
                    "bigDecimalVar",
                    bigDecimalScale3,
                    TaskStatus.COMPLETED
                );

                //then
                assertThat(retrievedTasks).extracting(Task::getName).containsExactly(taskScale3.getName());
            });
    }

    private <T> Collection<QueryCloudTask> executeGetTasksWithVariable(
        String variableName,
        T variableValue,
        TaskStatus status
    ) {
        return executeGetTasksWithVariable(TASKS_URL, variableName, variableValue, status);
    }

    private <T> Collection<QueryCloudTask> executeGetAdminTasksWithVariable(
        String variableName,
        T variableValue,
        TaskStatus status
    ) {
        return executeGetTasksWithVariable(ADMIN_TASKS_URL, variableName, variableValue, status);
    }

    private <T> Collection<QueryCloudTask> executeGetTasksWithVariable(
        String tasksUrl,
        String variableName,
        T variableValue,
        TaskStatus status
    ) {
        ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
            tasksUrl + "?variables.name={name}&variables.value={outcome}&variables.type={type}&status={status}",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE,
            variableName,
            variableValue,
            variableValue.getClass().getSimpleName().toLowerCase(),
            status
        );

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        return responseEntity.getBody().getContent();
    }

    private <T> PagedModel<QueryCloudTask> executeGetTasksWithVariablePagedRequest(
        String tasksUrl,
        String variableName,
        T variableValue,
        Pageable pageable
    ) {
        ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
            tasksUrl +
            "?variables.name={name}&variables.value={outcome}&variables.type={type}&" +
            queryStringFromPageable(pageable),
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE,
            variableName,
            variableValue,
            variableValue.getClass().getSimpleName().toLowerCase()
        );

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();

        return responseEntity.getBody();
    }

    @Test
    public void shouldGetTaskWithUpdatedInfo() {
        //given
        Task assignedTask = taskEventContainedBuilder.anAssignedTask(
            "Assigned task",
            "testuser",
            runningProcessInstance
        );

        eventsAggregator.sendAll();

        TaskImpl updatedTask = new TaskImpl(assignedTask.getId(), assignedTask.getName(), assignedTask.getStatus());
        updatedTask.setProcessInstanceId(assignedTask.getProcessInstanceId());
        updatedTask.setName("Updated name");
        updatedTask.setDescription("Updated description");
        updatedTask.setPriority(42);
        updatedTask.setFormKey("FormKey");

        //when
        producer.send(new CloudTaskUpdatedEventImpl(updatedTask));

        await()
            .untilAsserted(() -> {
                ResponseEntity<Task> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "/" + assignedTask.getId(),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    new ParameterizedTypeReference<Task>() {}
                );

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

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks();

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> task = responseEntity.getBody().getContent();
                assertThat(task)
                    .extracting(Task::getId, Task::getStatus, Task::getParentTaskId)
                    .contains(tuple(createdTask.getId(), Task.TaskStatus.CREATED, createdTask.getParentTaskId()));
            });
    }

    @Test
    public void shouldGetStandaloneAssignedTasksAndFilterParentId() {
        //given
        Task createdTask = taskEventContainedBuilder.aCreatedStandaloneAssignedTaskWithParent(
            "Created task with parent",
            "testuser"
        );

        eventsAggregator.sendAll();

        checkExistingTask(createdTask);
    }

    @Test
    public void shouldGetAssignedTasksAndFilterParentId() {
        //given
        Task createdTask = taskEventContainedBuilder.anAssignedTaskWithParent(
            "Created task with parent",
            "testuser",
            runningProcessInstance
        );

        eventsAggregator.sendAll();

        checkExistingTask(createdTask);
    }

    @Test
    public void shouldGetAvailableRootTasksWithStatus() {
        //given
        TaskImpl rootTaskNoSubtask = new TaskImpl(
            UUID.randomUUID().toString(),
            "Root task without subtask",
            Task.TaskStatus.ASSIGNED
        );
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

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?rootTasksOnly=true&status={status}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    Task.TaskStatus.CREATED
                );
                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getId).containsExactly(rootTask.getId());
            });
    }

    @Test
    public void shouldGetAvailableStandaloneTasksWithStatus() {
        //given
        TaskImpl processTask = new TaskImpl(UUID.randomUUID().toString(), "Task1", TaskStatus.CREATED);
        processTask.setProcessInstanceId(runningProcessInstance.getId());
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(processTask));

        TaskImpl standAloneTask = aCreatedTask("Task2");
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(standAloneTask));

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?standalone=true&status={status}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    Task.TaskStatus.CREATED
                );
                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getId).containsExactly(standAloneTask.getId());
            });
    }

    private void checkExistingTask(Task createdTask) {
        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks();

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> task = responseEntity.getBody().getContent();
                assertThat(task)
                    .extracting(Task::getId, Task::getStatus, Task::getParentTaskId)
                    .contains(tuple(createdTask.getId(), Task.TaskStatus.ASSIGNED, createdTask.getParentTaskId()));
            });
    }

    @Test
    public void shouldGetRestrictedTasksWithUserPermission() {
        //given
        identityTokenProducer.withTestUser("testuser");
        Task taskWithCandidate = taskEventContainedBuilder.aTaskWithUserCandidate(
            "task with candidate",
            "testuser",
            runningProcessInstance
        );
        //when
        eventsAggregator.sendAll();

        //then
        assertCanRetrieveTask(taskWithCandidate);
    }

    @Test
    public void shouldNotGetRestrictedTasksWithoutUserPermission() {
        //given
        Task taskWithCandidate = taskEventContainedBuilder.aTaskWithUserCandidate(
            "task with candidate",
            "specialUser",
            runningProcessInstance
        );

        //when
        eventsAggregator.sendAll();

        //then
        assertCannotSeeTask(taskWithCandidate);
    }

    @Test
    public void shouldGetRestrictedTasksWithGroupPermission() {
        //given
        //we are logged in as testuser who belongs to testgroup, so it should be able to see the task
        Task taskWithCandidate = taskEventContainedBuilder.aTaskWithGroupCandidate(
            "task with candidate",
            "testgroup",
            runningProcessInstance
        );

        //when
        eventsAggregator.sendAll();

        //then
        assertCanRetrieveTask(taskWithCandidate);
    }

    @Test
    public void shouldGetAdminTask() {
        //given
        //given
        Task createdTask = taskEventContainedBuilder.aCreatedTask("Created task", runningProcessInstance);
        eventsAggregator.sendAll();

        identityTokenProducer.withTestUser("hradmin");

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<Task> responseEntity = testRestTemplate.exchange(
                    ADMIN_TASKS_URL + "/" + createdTask.getId(),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    new ParameterizedTypeReference<Task>() {}
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getId()).isNotNull();
                assertThat(responseEntity.getBody().getId()).isEqualTo(createdTask.getId());
            });
    }

    private void assertCanRetrieveTask(Task task) {
        await()
            .untilAsserted(() -> {
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks();

                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks)
                    .extracting(Task::getId, Task::getStatus)
                    .contains(tuple(task.getId(), Task.TaskStatus.CREATED));
            });
    }

    @Test
    public void should_getTasksFilteredOnVariableNameAndValue_when_loggedAsAdmin() {
        //given
        taskEventContainedBuilder.aCompletedTask("Task with no var", runningProcessInstance);
        Task taskApproved = taskEventContainedBuilder.aCompletedTask("Task approved", runningProcessInstance);
        Task taskRejected = taskEventContainedBuilder.aCompletedTask("Task rejected", runningProcessInstance);
        Task createdTask = taskEventContainedBuilder.aCreatedTask("Task Created", runningProcessInstance);

        variableEventContainedBuilder.aCreatedVariable("outcome", "approved").onTask(taskApproved);
        variableEventContainedBuilder.aCreatedVariable("intValue", 40).onTask(taskApproved);
        variableEventContainedBuilder.aCreatedVariable("outcome", "approved").onTask(createdTask);
        variableEventContainedBuilder.aCreatedVariable("outcome", "rejected").onTask(taskRejected);
        variableEventContainedBuilder.aCreatedVariable("anotherVariable", "approved").onTask(taskRejected);

        eventsAggregator.sendAll();

        identityTokenProducer.withTestUser("hradmin");

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> approvedTasks = executeGetAdminTasksWithVariable(
                    "outcome",
                    "approved",
                    TaskStatus.COMPLETED
                );

                //then
                assertThat(approvedTasks).extracting(Task::getName).containsExactly(taskApproved.getName());
            });
    }

    @Test
    public void shouldNotGetRestrictedTasksWithoutGroupPermission() {
        //given
        //we are logged in as test user who does not belong to hrgroup, so it should not be available
        Task taskWithCandidate = taskEventContainedBuilder.aTaskWithGroupCandidate(
            "task with candidate",
            "hrgroup",
            runningProcessInstance
        );
        //when
        eventsAggregator.sendAll();

        //then
        assertCannotSeeTask(taskWithCandidate);
    }

    @Test
    public void shouldGetAddRemoveTaskUserCandidates() {
        //given
        Task createdTask = taskEventContainedBuilder.aTaskWithUserCandidate(
            "task with user candidate",
            "testuser",
            runningProcessInstance
        );
        eventsAggregator.sendAll();

        identityTokenProducer.withTestUser("testuser");

        //when
        await()
            .untilAsserted(() -> {
                ResponseEntity<List<String>> responseEntity = getCandidateUsers(createdTask.getId());

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().size()).isEqualTo(1);
                assertThat(responseEntity.getBody().get(0)).isEqualTo("testuser");
            });

        //Check adding user candidate
        //when
        TaskCandidateUserImpl addCandidateUser = new TaskCandidateUserImpl("hruser", createdTask.getId());
        producer.send(new CloudTaskCandidateUserAddedEventImpl(addCandidateUser));

        //then
        await()
            .untilAsserted(() -> {
                ResponseEntity<List<String>> responseEntity = getCandidateUsers(createdTask.getId());

                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().size()).isEqualTo(2);
                assertThat(responseEntity.getBody().get(0)).isIn("hruser", "testuser");
                assertThat(responseEntity.getBody().get(1)).isIn("hruser", "testuser");
            });

        //Check deleting user candidate
        //when
        TaskCandidateUserImpl deleteCandidateUser = new TaskCandidateUserImpl("hruser", createdTask.getId());
        producer.send(new CloudTaskCandidateUserRemovedEventImpl(deleteCandidateUser));

        //then
        await()
            .untilAsserted(() -> {
                ResponseEntity<List<String>> responseEntity = getCandidateUsers(createdTask.getId());

                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().size()).isEqualTo(1);
                assertThat(responseEntity.getBody().get(0)).isEqualTo("testuser");
            });
    }

    @Test
    public void shouldGetAddRemoveTaskGroupCandidates() {
        //given
        Task createdTask = taskEventContainedBuilder.aTaskWithGroupCandidate(
            "task with group candidate",
            "testgroup",
            runningProcessInstance
        );
        eventsAggregator.sendAll();

        identityTokenProducer.withTestUser("testuser");

        //when
        await()
            .untilAsserted(() -> {
                ResponseEntity<List<String>> responseEntity = getCandidateGroups(createdTask.getId());
                ResponseEntity<Task> taskResponseEntity = executeRequestGetTasksById(createdTask.getId());

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().size()).isEqualTo(1);
                assertThat(responseEntity.getBody()).containsExactly("testgroup");
                assertThat(taskResponseEntity.getBody().getCandidateGroups()).containsExactly("testgroup");
            });

        //Check adding group candidate
        //when
        TaskCandidateGroupImpl addCandidateGroup = new TaskCandidateGroupImpl("hrgroup", createdTask.getId());
        producer.send(new CloudTaskCandidateGroupAddedEventImpl(addCandidateGroup));

        //then
        await()
            .untilAsserted(() -> {
                ResponseEntity<List<String>> responseEntity = getCandidateGroups(createdTask.getId());
                ResponseEntity<Task> taskResponseEntity = executeRequestGetTasksById(createdTask.getId());

                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().size()).isEqualTo(2);
                assertThat(responseEntity.getBody()).containsExactlyInAnyOrder("hrgroup", "testgroup");
                assertThat(taskResponseEntity.getBody().getCandidateGroups())
                    .containsExactlyInAnyOrder("hrgroup", "testgroup");
            });

        //Check deleting group candidate
        //when
        TaskCandidateGroupImpl deleteCandidateGroup = new TaskCandidateGroupImpl("hrgroup", createdTask.getId());
        producer.send(new CloudTaskCandidateGroupRemovedEventImpl(deleteCandidateGroup));

        //then
        await()
            .untilAsserted(() -> {
                ResponseEntity<List<String>> responseEntity = getCandidateGroups(createdTask.getId());
                ResponseEntity<Task> taskResponseEntity = executeRequestGetTasksById(createdTask.getId());

                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().size()).isEqualTo(1);
                assertThat(responseEntity.getBody()).containsExactly("testgroup");
                assertThat(taskResponseEntity.getBody().getCandidateGroups()).containsExactly("testgroup");
            });
    }

    @Test
    public void adminShouldAssignTask() {
        //given
        Task createdTask = taskEventContainedBuilder.aCreatedTask("Created task", runningProcessInstance);
        eventsAggregator.sendAll();

        identityTokenProducer.withTestUser("hradmin");

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(
                    ADMIN_TASKS_URL + "/" + createdTask.getId(),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    new ParameterizedTypeReference<CloudTask>() {}
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getId()).isEqualTo(createdTask.getId());
                assertThat(responseEntity.getBody().getAssignee()).isNull();

                //when
                TaskImpl assignedTask = new TaskImpl(
                    createdTask.getId(),
                    createdTask.getName(),
                    createdTask.getStatus()
                );
                assignedTask.setAssignee("hruser");

                producer.send(new CloudTaskAssignedEventImpl(assignedTask));

                //then
                responseEntity =
                    testRestTemplate.exchange(
                        ADMIN_TASKS_URL + "/" + createdTask.getId(),
                        HttpMethod.GET,
                        identityTokenProducer.entityWithAuthorizationHeader(),
                        new ParameterizedTypeReference<CloudTask>() {}
                    );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getId()).isNotNull();
                assertThat(responseEntity.getBody().getId()).isEqualTo(createdTask.getId());
                assertThat(responseEntity.getBody().getAssignee()).isEqualTo("hruser");

                //Restore user
                identityTokenProducer.withTestUser("testuser");
            });
    }

    @Test
    public void shouldGetCorrectCompletedDateAndDurationWhenCompleted() {
        //given

        Date now = new Date(System.currentTimeMillis());
        Date yesterday = new Date(System.currentTimeMillis() - 86400000);

        Task completedTask = taskEventContainedBuilder.aCompletedTaskWithCreationDateAndCompletionDate(
            "Completed task",
            runningProcessInstance,
            yesterday,
            now
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
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
        await()
            .untilAsserted(() -> {
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks();

                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                //don't see the task as not for me
                assertThat(tasks).extracting(Task::getId).doesNotContain(task.getId());
            });
    }

    private ResponseEntity<PagedModel<QueryCloudTask>> executeRequestGetTasksFiltered(String name, String description) {
        String url = TASKS_URL;
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
        return testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE
        );
    }

    private ResponseEntity<PagedModel<QueryCloudTask>> executeRequestGetTasks() {
        return testRestTemplate.exchange(
            TASKS_URL,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE
        );
    }

    private ResponseEntity<PagedModel<QueryCloudTask>> executeRequestGetTasksWithProcessVariables(
        String... variableKeys
    ) {
        return testRestTemplate.exchange(
            TASKS_URL + "?variableKeys=" + String.join(",", variableKeys),
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE
        );
    }

    private ResponseEntity<PagedModel<QueryCloudTask>> executeRequestGetTasksWithProcessVariables(
        ProcessInstance processInstance,
        String... variableKeys
    ) {
        return testRestTemplate.exchange(
            "/v1/process-instances/{processInstanceId}/tasks?variableKeys=" + String.join(",", variableKeys),
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE,
            processInstance.getId()
        );
    }

    private ResponseEntity<PagedModel<Task>> executeRequestGetAdminTasks(ProcessInstance processInstance) {
        return testRestTemplate.exchange(
            "/admin/v1/process-instances/{processInstanceId}/tasks",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASK_INTERFACE_RESPONSE_TYPE,
            processInstance.getId()
        );
    }

    private ResponseEntity<PagedModel<QueryCloudTask>> executeRequestGetTasks(ProcessInstance processInstance) {
        return testRestTemplate.exchange(
            "/v1/process-instances/{processInstanceId}/tasks",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE,
            processInstance.getId()
        );
    }

    private ResponseEntity<Task> executeRequestGetAdminTasksById(String id) {
        return testRestTemplate.exchange(
            ADMIN_TASKS_URL + "/" + id,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            SINGLE_TASK_RESPONSE_TYPE
        );
    }

    private ResponseEntity<Task> executeRequestGetTasksById(String id) {
        return testRestTemplate.exchange(
            TASKS_URL + "/" + id,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            SINGLE_TASK_RESPONSE_TYPE
        );
    }

    private ResponseEntity<List<String>> getCandidateUsers(String taskId) {
        return testRestTemplate.exchange(
            TASKS_URL + "/" + taskId + "/candidate-users",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            new ParameterizedTypeReference<List<String>>() {}
        );
    }

    private ResponseEntity<List<String>> getCandidateGroups(String taskId) {
        return testRestTemplate.exchange(
            TASKS_URL + "/" + taskId + "/candidate-groups",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            new ParameterizedTypeReference<List<String>>() {}
        );
    }

    @Test
    public void shouldFilterTaskByCreatedDateFromTo() {
        //given
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date now = new Date();
        Date start1, complete1, start2, complete2, start3, complete3;

        complete1 = now;
        start1 = new Date(now.getTime() - 86400000);
        Task task1 = taskEventContainedBuilder.aCompletedTaskWithCreationDateAndCompletionDate(
            "Completed task 1",
            runningProcessInstance,
            start1,
            complete1
        );

        complete2 = new Date(now.getTime() - 86400000);
        start2 = new Date(now.getTime() - (2 * 86400000));
        Task task2 = taskEventContainedBuilder.aCompletedTaskWithCreationDateAndCompletionDate(
            "Completed task 2",
            runningProcessInstance,
            start2,
            complete2
        );

        complete3 = new Date(now.getTime() - (3 * 86400000));
        start3 = new Date(now.getTime() - (4 * 86400000));
        Task task3 = taskEventContainedBuilder.aCompletedTaskWithCreationDateAndCompletionDate(
            "Completed task 3",
            runningProcessInstance,
            start3,
            complete3
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                //set check date 1 hour back from start2: we expect 2 tasks
                Date checkDate = new Date(start2.getTime() - 3600000);
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?createdFrom=" + sdf.format(checkDate),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE
                );
                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks.size()).isEqualTo(2);

                //when
                //set check date 1 hour after start2: we expect 1 task
                checkDate = new Date(start2.getTime() + 3600000);
                responseEntity =
                    testRestTemplate.exchange(
                        TASKS_URL + "?createdFrom=" + sdf.format(checkDate),
                        HttpMethod.GET,
                        identityTokenProducer.entityWithAuthorizationHeader(),
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
                checkDate = new Date(start2.getTime() + 3600000);
                responseEntity =
                    testRestTemplate.exchange(
                        TASKS_URL + "?createdTo=" + sdf.format(checkDate),
                        HttpMethod.GET,
                        identityTokenProducer.entityWithAuthorizationHeader(),
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
                checkDate = new Date(start2.getTime() - 3600000);
                Date checkDate1 = new Date(start2.getTime() + 3600000);

                responseEntity =
                    testRestTemplate.exchange(
                        TASKS_URL + "?createdFrom=" + sdf.format(checkDate) + "&createdTo=" + sdf.format(checkDate1),
                        HttpMethod.GET,
                        identityTokenProducer.entityWithAuthorizationHeader(),
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
    public void shouldHaveCreatedStatusAfterBeingReleased() {
        Task releasedTask = taskEventContainedBuilder.aReleasedTask("released-task");

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
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
        Task task1 = taskEventContainedBuilder.aCreatedTask("Task 1 for filter", runningProcessInstance);
        Task task2 = taskEventContainedBuilder.aCreatedTask("Task 2 not filter", null);

        Task task3 = taskEventContainedBuilder.aCreatedTask("Task 3 for filter standalone", null);

        TaskImpl task4 = aCreatedTask("Task 4 for filter description");
        task4.setDescription("My task description");
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task4));

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasksFiltered(
                    "for filter",
                    null
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> task = responseEntity.getBody().getContent();
                assertThat(task)
                    .extracting(Task::getId, Task::getStatus)
                    .contains(
                        tuple(task1.getId(), Task.TaskStatus.CREATED),
                        tuple(task3.getId(), Task.TaskStatus.CREATED),
                        tuple(task4.getId(), Task.TaskStatus.CREATED)
                    );
            });

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasksFiltered(
                    "for filter",
                    "task descr"
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> task = responseEntity.getBody().getContent();
                assertThat(task)
                    .extracting(Task::getId, Task::getStatus)
                    .contains(tuple(task4.getId(), Task.TaskStatus.CREATED));
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

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks();

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks)
                    .extracting(Task::getId, Task::getStatus, Task::getProcessDefinitionVersion, Task::getBusinessKey)
                    .contains(
                        tuple(task1.getId(), Task.TaskStatus.CREATED, 10, "businessKey"),
                        tuple(task2.getId(), Task.TaskStatus.CREATED, null, null)
                    );
            });

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?processDefinitionVersion={processDefinitionVersion}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    10
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getId).containsExactly(task1.getId());
            });
    }

    @Test
    public void should_getTask_when_queryFilteredByTaskDefinitionKey() {
        //given
        CloudTaskCreatedEventImpl task1Created = buildTaskCreatedEvent("Task1", "taskDefinitionKey");
        CloudTaskCreatedEventImpl task2Created = buildTaskCreatedEvent("Task2", null);
        eventsAggregator.addEvents(task1Created);
        eventsAggregator.addEvents(task2Created);
        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks();

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks)
                    .extracting(Task::getId, Task::getStatus, Task::getTaskDefinitionKey)
                    .contains(
                        tuple(task1Created.getEntity().getId(), Task.TaskStatus.CREATED, "taskDefinitionKey"),
                        tuple(task2Created.getEntity().getId(), Task.TaskStatus.CREATED, null)
                    );
            });

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?taskDefinitionKey={taskDefinitionKey}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    "taskDefinitionKey"
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getId).containsExactly(task1Created.getEntity().getId());
            });
    }

    private CloudTaskCreatedEventImpl buildTaskCreatedEvent(String taskName, String taskDefinitionKey) {
        TaskImpl task1 = aCreatedTask(taskName, taskDefinitionKey);

        return new CloudTaskCreatedEventImpl(task1);
    }

    private TaskImpl aCreatedTask(String taskName, String taskDefinitionKey) {
        TaskImpl task = aCreatedTask(taskName);
        task.setTaskDefinitionKey(taskDefinitionKey);
        return task;
    }

    private TaskImpl aCreatedTask(String taskName) {
        return new TaskImpl(UUID.randomUUID().toString(), taskName, Task.TaskStatus.CREATED);
    }

    @Test
    public void shouldGetTaskGroupCandidatesAfterTaskCompleted() {
        //given
        Task task = taskEventContainedBuilder.aTaskWithGroupCandidate(
            "task with group candidate",
            "testgroup",
            runningProcessInstance
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
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

        identityTokenProducer.withTestUser("testuser");

        ((TaskImpl) task).setAssignee("testuser");
        ((TaskImpl) task).setStatus(Task.TaskStatus.ASSIGNED);
        eventsAggregator.addEvents(new CloudTaskAssignedEventImpl(task));
        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
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

        ((TaskImpl) task).setStatus(Task.TaskStatus.COMPLETED);
        eventsAggregator.addEvents(
            new CloudTaskCompletedEventImpl(UUID.randomUUID().toString(), new Date().getTime(), task)
        );
        eventsAggregator.sendAll();

        TaskCandidateGroupImpl candidateGroup = new TaskCandidateGroupImpl("hrgroup", task.getId());
        producer.send(new CloudTaskCandidateGroupRemovedEventImpl(candidateGroup));

        await()
            .untilAsserted(() -> {
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
        Task task = taskEventContainedBuilder.aTaskWithUserCandidate(
            "task with user candidate",
            "testuser",
            runningProcessInstance
        );
        eventsAggregator.sendAll();

        identityTokenProducer.withTestUser("testuser");

        //when
        await()
            .untilAsserted(() -> {
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
            });

        //when
        ((TaskImpl) task).setAssignee("testuser");
        ((TaskImpl) task).setStatus(Task.TaskStatus.ASSIGNED);
        eventsAggregator.addEvents(new CloudTaskAssignedEventImpl(task));
        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
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
            });

        ((TaskImpl) task).setStatus(Task.TaskStatus.COMPLETED);
        eventsAggregator.addEvents(
            new CloudTaskCompletedEventImpl(UUID.randomUUID().toString(), new Date().getTime(), task)
        );
        eventsAggregator.sendAll();

        TaskCandidateUserImpl candidateUser = new TaskCandidateUserImpl("testuser", task.getId());
        producer.send(new CloudTaskCandidateUserRemovedEventImpl(candidateUser));

        //then
        await()
            .untilAsserted(() -> {
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
            });
    }

    @Test
    public void shouldFilterTasksForDueDate() {
        //given
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date dueDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date now = cal.getTime();

        //set due date as current date + 1
        dueDate.setTime(now.getTime() + Duration.ofDays(1).toMillis());

        Task assignedTask1 = taskEventContainedBuilder.anAssignedTaskWithDueDate(
            "Assigned task1",
            "testuser",
            runningProcessInstance,
            new Date(now.getTime() - Duration.ofDays(1).toMillis())
        );
        Task assignedTask2 = taskEventContainedBuilder.anAssignedTaskWithDueDate(
            "Assigned task2",
            "testuser",
            runningProcessInstance,
            dueDate
        );
        Task assignedTask3 = taskEventContainedBuilder.anAssignedTaskWithDueDate(
            "Assigned task3",
            "testuser",
            runningProcessInstance,
            new Date(dueDate.getTime() + Duration.ofDays(2).toMillis())
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                //set check date to current date
                Date fromDate = now;
                // to date, from date plus 2 days
                Date toDate = new Date(dueDate.getTime() + Duration.ofDays(1).toMillis());
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?dueDateFrom=" + sdf.format(fromDate) + "&dueDateTo=" + sdf.format(toDate),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE
                );
                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getName).containsExactly(assignedTask2.getName());
            });

        await()
            .untilAsserted(() -> {
                //check for specific due date
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?dueDate=" + sdf.format(dueDate),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getName).containsExactly(assignedTask2.getName());
            });
    }

    @Test
    public void should_getTask_when_queryFilteredByProcessDefinitionName() {
        //given
        Task task1 = taskEventContainedBuilder.aCreatedTask("Task1", runningProcessInstance);

        Task task2 = taskEventContainedBuilder.aCreatedTask("Task2", null);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks();

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getId).contains(task1.getId(), task2.getId());
            });

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?processDefinitionName={processDefinitionName}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    "my-proc-definition-name"
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getId).containsExactly(task1.getId());
            });
    }

    @Test
    public void should_notGetTasks_by_ProcessInstance_userIsNotInvolved() {
        identityTokenProducer.withTestUser(HRUSER);
        taskEventContainedBuilder.aTaskWithUserCandidate("Task1", "fakeUser", runningProcessInstance);

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks(
                    runningProcessInstance
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).isEmpty();
            });
    }

    @Test
    public void should_getTasks_by_ProcessInstance_when_userIsCandidate() {
        identityTokenProducer.withTestUser(HRUSER);
        //given
        QueryCloudTask task1 = taskEventContainedBuilder.aQueryCloudTaskWithUserCandidate(
            "Task1",
            TESTUSER,
            runningProcessInstance
        );

        QueryCloudTask task2 = taskEventContainedBuilder.aQueryCloudTaskWithUserCandidate(
            "Task2",
            HRUSER,
            runningProcessInstance
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks(
                    runningProcessInstance
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).containsExactlyInAnyOrder(task1, task2);
            });

        assertCanRetrieveTaskById(task1.getId());
        assertCanRetrieveTaskById(task2.getId());
    }

    @Test
    public void should_getTasks_by_ProcessInstance_when_userIsAssignee() {
        identityTokenProducer.withTestUser(HRUSER);

        //given
        QueryCloudTask task1 = taskEventContainedBuilder.aQueryCloudTaskWithUserCandidate(
            "Task1",
            TESTUSER,
            runningProcessInstance
        );

        QueryCloudTask task2 = taskEventContainedBuilder.anAssignedQueryCloudTask(
            "Task2",
            HRUSER,
            runningProcessInstance
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks(
                    runningProcessInstance
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).containsExactlyInAnyOrder(task1, task2);
            });

        assertCanRetrieveTaskById(task1.getId());
        assertCanRetrieveTaskById(task2.getId());
    }

    @Test
    public void should_getTasks_by_ProcessInstance_when_userIsInGroupCandidate() {
        identityTokenProducer.withTestUser(HRUSER);
        //given
        QueryCloudTask task1 = taskEventContainedBuilder.aQueryCloudTaskWithUserCandidate(
            "Task1",
            TESTUSER,
            runningProcessInstance
        );

        QueryCloudTask task2 = taskEventContainedBuilder.aQueryCloudTaskWithGroupCandidate(
            "Task2",
            "hr",
            runningProcessInstance
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks(
                    runningProcessInstance
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).containsExactlyInAnyOrder(task1, task2);
            });
        assertCanRetrieveTaskById(task1.getId());
        assertCanRetrieveTaskById(task2.getId());
    }

    @Test
    public void should_getTasks_by_ProcessInstance_when_userIsInitiator() {
        //given
        QueryCloudTask task1 = taskEventContainedBuilder.aQueryCloudTaskWithUserCandidate(
            "Task1",
            HRUSER,
            runningProcessInstance
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks(
                    runningProcessInstance
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks.size()).isEqualTo(1);
                assertThat(tasks).contains(task1);
            });
        assertCanRetrieveTaskById(task1.getId());
    }

    @Test
    public void should_getAllTasks_by_ProcessInstance_whenUserIsAdmin() {
        //given
        Task task1 = taskEventContainedBuilder.aTaskWithUserCandidate("Task1", "user", runningProcessInstance);
        Task task2 = taskEventContainedBuilder.aTaskWithGroupCandidate("Task2", "group", runningProcessInstance);
        Task task3 = taskEventContainedBuilder.anAssignedTask("Task3", TESTUSER, runningProcessInstance);
        identityTokenProducer.withTestUser("hradmin");

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<Task>> responseEntity = executeRequestGetAdminTasks(runningProcessInstance);

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<Task> tasks = responseEntity.getBody().getContent();
                assertThat(tasks.size()).isEqualTo(3);
                assertThat(tasks).contains(task1, task2, task3);
            });
        assertAdminCanRetrieveTaskById(task1.getId());
        assertAdminCanRetrieveTaskById(task2.getId());
        assertAdminCanRetrieveTaskById(task3.getId());

        identityTokenProducer.withTestUser(TESTUSER);
    }

    private void assertCanRetrieveTaskById(String taskId) {
        await()
            .untilAsserted(() -> {
                ResponseEntity<Task> responseEntity = executeRequestGetTasksById(taskId);

                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Task task = responseEntity.getBody();
                assertThat(task.getId()).isEqualTo(taskId);
            });
    }

    private void assertAdminCanRetrieveTaskById(String taskId) {
        await()
            .untilAsserted(() -> {
                ResponseEntity<Task> responseEntity = executeRequestGetAdminTasksById(taskId);

                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Task task = responseEntity.getBody();
                assertThat(task.getId()).isEqualTo(taskId);
            });
    }

    @Test
    public void should_getTasks_withCandidateUsersAndGroups_by_ProcessInstance() {
        //given
        Task task1 = taskEventContainedBuilder.aTaskWithUserCandidate("Task1", "testuser", runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = executeRequestGetTasks(
                    runningProcessInstance
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks)
                    .flatExtracting(Task::getName, Task::getCandidateUsers, Task::getCandidateGroups)
                    .contains(task1.getName(), Collections.singletonList("testuser"), Collections.emptyList());
            });
    }

    @Test
    public void shouldFilterTasksForCompletedBy() {
        //Given
        String completedByFirstUser = "hruser1";
        String completedBySecondUser = "userXyz";

        Task assignedTask = taskEventContainedBuilder.aCompletedTaskWithCompletedBy(
            "Assigned task1",
            runningProcessInstance,
            completedByFirstUser
        );
        taskEventContainedBuilder.aCompletedTaskWithCompletedBy(
            "Assigned task2",
            runningProcessInstance,
            completedBySecondUser
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //check for specific completed by value
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?completedBy=" + completedByFirstUser,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getCompletedBy).containsExactly(assignedTask.getCompletedBy());
            });
    }

    @Test
    public void should_getTask_when_queryFilteredByPriority() {
        //given
        Task task1 = taskEventContainedBuilder.aTaskWithPriority("Task1", 20, runningProcessInstance);

        taskEventContainedBuilder.aTaskWithPriority("Task2", 30, runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?priority={priority}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    "20"
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getId).containsExactly(task1.getId());
            });
    }

    @Test
    public void shouldGetTaskListFilteredByCompletedDate() {
        //given
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date completedDateToday = new Date();
        Date completedDateTwoDaysAgo = new Date();
        Date completedDateFiveDaysAfter = new Date();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date now = cal.getTime();

        //Start a Task and set it's completed date as current date
        completedDateToday.setTime(now.getTime());
        Task task1 = taskEventContainedBuilder.aCompletedTaskWithCompletionDate(
            "Task1",
            runningProcessInstance,
            completedDateToday
        );

        //Start a Task and set it's completed date as current date minus two days
        completedDateTwoDaysAgo.setTime(now.getTime() - Duration.ofDays(2).toMillis());
        taskEventContainedBuilder.aCompletedTaskWithCompletionDate(
            "Task2",
            runningProcessInstance,
            completedDateTwoDaysAgo
        );

        //Start a Task and set it's completed date as current date plus five days
        completedDateFiveDaysAfter.setTime(now.getTime() + Duration.ofDays(5).toMillis());
        taskEventContainedBuilder.aCompletedTaskWithCompletionDate(
            "Task3",
            runningProcessInstance,
            completedDateFiveDaysAfter
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                //set from date to yesterday date
                Date fromDate = new Date(now.getTime() - Duration.ofDays(1).toMillis());
                // to date, from date plus 2 days
                Date toDate = new Date(now.getTime() + Duration.ofDays(2).toMillis());
                //when
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?completedFrom=" + sdf.format(fromDate) + "&completedTo=" + sdf.format(toDate),
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE
                );

                //then
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                Collection<QueryCloudTask> filteredTaskEntities = responseEntity.getBody().getContent();
                assertThat(filteredTaskEntities).extracting(Task::getId).containsExactly(task1.getId());
            });
    }

    @Test
    public void shouldFilterTasksBySingleCandidateGroupIdOrListOfCandidateGroupIds() {
        //given
        Task firstTaskWithCandidateGroupInFilter = taskEventContainedBuilder.aTaskWithGroupCandidate(
            "task one",
            "testgroup",
            runningProcessInstance
        );
        Task taskWithCandidateGroupNotInFilter = taskEventContainedBuilder.aTaskWithGroupCandidate(
            "task two",
            "testgroup2",
            runningProcessInstance
        );
        Task secondTaskWithCandidateGroupInFilter = taskEventContainedBuilder.aTaskWithTwoGroupCandidates(
            "task three",
            "hrgroup",
            "testgroup4",
            runningProcessInstance,
            "testuser"
        );
        //when
        eventsAggregator.sendAll();

        //then
        //query for single candidate groudId
        await()
            .untilAsserted(() -> {
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?candidateGroupId=testgroup",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE
                );
                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks).extracting(Task::getId).containsExactly(firstTaskWithCandidateGroupInFilter.getId());
            });

        //query for multiple candidate groudIds
        await()
            .untilAsserted(() -> {
                ResponseEntity<PagedModel<QueryCloudTask>> responseEntity = testRestTemplate.exchange(
                    TASKS_URL + "?candidateGroupId=testgroup,hrgroup",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE
                );

                assertThat(responseEntity).isNotNull();
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

                assertThat(responseEntity.getBody()).isNotNull();
                Collection<QueryCloudTask> tasks = responseEntity.getBody().getContent();
                assertThat(tasks)
                    .extracting(Task::getId)
                    .containsExactly(
                        firstTaskWithCandidateGroupInFilter.getId(),
                        secondTaskWithCandidateGroupInFilter.getId()
                    );
            });
    }

    @Test
    public void should_getCompletedTaskWithProcessVariables() {
        //given
        taskEventContainedBuilder.aCompletedTask("Task", runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactly(tuple("Task", "varAName", "varAValue"));
            });
    }

    @Test
    public void should_getCreatedTaskWithProcessVariables() {
        //given
        taskEventContainedBuilder.aCreatedTask("Task", runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactly(tuple("Task", "varAName", "varAValue"));
            });
    }

    @Test
    public void should_getCreatedTaskWithPreviouslyCreatedProcessVariables() {
        //given
        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);
        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        taskEventContainedBuilder.aCreatedTask("Task", runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactly(tuple("Task", "varAName", "varAValue"));
            });
    }

    @Test
    public void should_getOnlyTasksWithProcessVariablesRequested() {
        //given
        final ProcessInstance otherProcessInstance = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "ProcessInstanceWithInitiator",
            TESTUSER
        );
        taskEventContainedBuilder.aCreatedTask("Created task", runningProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Completed task", runningProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Other completed task", otherProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Other created task", otherProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactlyInAnyOrder(
                        tuple("Created task", "varAName", "varAValue"),
                        tuple("Completed task", "varAName", "varAValue"),
                        tuple("Other completed task", null, null),
                        tuple("Other created task", null, null)
                    );
            });
    }

    @Test
    public void should_notGetProcessVariablesWhenNotRequested() {
        //given
        final ProcessInstance otherProcessInstance = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "ProcessInstanceWithInitiator",
            TESTUSER
        );
        taskEventContainedBuilder.aCreatedTask("Created task", runningProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Completed task", runningProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Other completed task", otherProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Other created task", otherProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasks().getBody().getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        QueryCloudTask -> CollectionUtils.isEmpty((QueryCloudTask.getProcessVariables()))
                    )
                    .containsExactly(
                        tuple("Created task", true),
                        tuple("Completed task", true),
                        tuple("Other completed task", true),
                        tuple("Other created task", true)
                    );
            });
    }

    private Function<QueryCloudTask, Object> getProcessVariableField(Function<CloudVariableInstance, ?> function) {
        return queryCloudTask -> queryCloudTask.getProcessVariables().stream().map(function).findFirst().orElse(null);
    }

    @Test
    public void should_getCompletedTaskWithProcessVariablesForProcessInstance() {
        //given
        taskEventContainedBuilder.aCompletedTask("Task", runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    runningProcessInstance,
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactly(tuple("Task", "varAName", "varAValue"));
            });
    }

    @Test
    public void should_getCreatedTaskWithProcessVariablesForProcessInstance() {
        //given
        taskEventContainedBuilder.aCreatedTask("Task", runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    runningProcessInstance,
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactly(tuple("Task", "varAName", "varAValue"));
            });
    }

    @Test
    public void should_getCreatedTaskWithPreviouslyCreatedProcessVariablesForProcessInstance() {
        //given
        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);
        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        taskEventContainedBuilder.aCreatedTask("Task", runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    runningProcessInstance,
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactly(tuple("Task", "varAName", "varAValue"));
            });
    }

    @Test
    public void should_getOnlyTasksWithProcessVariablesRequestedForProcessInstance() {
        //given
        final ProcessInstance otherProcessInstance = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "ProcessInstanceWithInitiator",
            TESTUSER
        );
        taskEventContainedBuilder.aCreatedTask("Created task", runningProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Completed task", runningProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Other completed task", otherProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Other created task", otherProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    runningProcessInstance,
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactlyInAnyOrder(
                        tuple("Created task", "varAName", "varAValue"),
                        tuple("Completed task", "varAName", "varAValue")
                    );
            });
    }

    @Test
    public void should_notGetProcessVariablesWhenNotRequestedForProcessInstance() {
        //given
        final ProcessInstance otherProcessInstance = processInstanceBuilder.aRunningProcessInstanceWithInitiator(
            "ProcessInstanceWithInitiator",
            TESTUSER
        );
        taskEventContainedBuilder.aCreatedTask("Created task", runningProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Completed task", runningProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Other completed task", otherProcessInstance);
        taskEventContainedBuilder.aCompletedTask("Other created task", otherProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasks(runningProcessInstance)
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        QueryCloudTask -> CollectionUtils.isEmpty((QueryCloudTask.getProcessVariables()))
                    )
                    .containsExactly(tuple("Created task", true), tuple("Completed task", true));
            });
    }

    @Test
    public void should_migrateTaskProcessVariables() throws Exception {
        //given
        Task task = taskEventContainedBuilder.aCompletedTask("Task", runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varAName", "varAValue", "string", "varAProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariableWithProcessDefinitionKey("varBName", "varBValue", "string", "varBProcessDefinitionKey")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactly(tuple("Task", "varAName", "varAValue"));
            });

        Long taskProcessVariableCount = processVariablesMigrationHelper.getTaskProcessVariableCount(task.getId());
        assertThat(taskProcessVariableCount).isEqualTo(2);

        processVariablesMigrationHelper.deleteFromTaskProcessVariable(task.getId());
        taskProcessVariableCount = processVariablesMigrationHelper.getTaskProcessVariableCount(task.getId());
        assertThat(taskProcessVariableCount).isEqualTo(0);

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(Task::getName, tasks -> tasks.getProcessVariables().size())
                    .containsExactly(tuple("Task", 0));
            });

        processVariablesMigrationHelper.migrateTaskProcessVariableData();
        taskProcessVariableCount = processVariablesMigrationHelper.getTaskProcessVariableCount(task.getId());
        assertThat(taskProcessVariableCount).isEqualTo(2);

        await()
            .untilAsserted(() -> {
                //when
                Collection<QueryCloudTask> retrievedTasks = executeRequestGetTasksWithProcessVariables(
                    "varAProcessDefinitionKey/varAName"
                )
                    .getBody()
                    .getContent();

                //then
                assertThat(retrievedTasks)
                    .extracting(
                        Task::getName,
                        getProcessVariableField(VariableInstance::getName),
                        getProcessVariableField(VariableInstance::getValue)
                    )
                    .containsExactly(tuple("Task", "varAName", "varAValue"));
            });
    }

    @Test
    public void shouldGetRestrictedTasksWithUserPermissionOnDuplicateCandidateEvents() {
        //given
        identityTokenProducer.withTestUser(TESTUSER);
        Task taskWithCandidate = taskEventContainedBuilder.aTaskWithUserCandidate(
            "task with duplicate candidate event",
            TESTUSER,
            runningProcessInstance
        );

        //sending same task candidate again, shouldn't cause a problem with the query
        eventsAggregator.addEvents(
            new CloudTaskCandidateUserAddedEventImpl(new TaskCandidateUserImpl(TESTUSER, taskWithCandidate.getId()))
        );
        //when
        eventsAggregator.sendAll();

        //then
        assertCanRetrieveTask(taskWithCandidate);
    }

    private ResponseEntity<PagedModel<QueryCloudTask>> executeRequestGetTasks(Pageable pageable) {
        return testRestTemplate.exchange(
            TASKS_URL + "?".concat(queryStringFromPageable(pageable)),
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE
        );
    }

    private static String encodeURLComponent(Object component) {
        try {
            return URLEncoder.encode(component.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String queryStringFromPageable(Pageable pageable) {
        StringBuilder result = new StringBuilder();
        result.append("page=").append(encodeURLComponent(pageable.getPageNumber()));
        result.append("&size=").append(pageable.getPageSize());

        // No sorting
        if (pageable.getSort().isUnsorted()) {
            return result.toString();
        }

        // Sorting is specified
        for (Sort.Order o : pageable.getSort()) {
            result.append("&sort=");
            result.append(encodeURLComponent(o.getProperty()));
            result.append(",");
            result.append(encodeURLComponent(o.getDirection().name()));
        }

        return result.toString();
    }
}
