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

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.types.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.activiti.cloud.services.query.rest.predicate.RootTasksFilter;
import org.activiti.cloud.services.query.rest.predicate.StandAloneTaskFilter;
import org.activiti.cloud.util.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true",
        "logging.level.org.hibernate.collection.spi=warn",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true",
    }
)
@Testcontainers
@TestPropertySource("classpath:application-test.properties")
public class TaskControllerHelperIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    TaskControllerHelper taskControllerHelper;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskVariableRepository taskVariableRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        taskVariableRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
    }

    @Test
    void should_returnTasks_withProcessVariablesByKeys() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> taskEntities = createTasks(processInstanceEntity, 10);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<String> processVariableKeys = IntStream
            .range(0, variables.size())
            .filter(i -> i % 2 == 0)
            .mapToObj(i -> processInstanceEntity.getProcessDefinitionKey() + "/name" + i)
            .toList();

        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys
        );

        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();

        assertThat(retrievedTasks)
            .extracting(QueryCloudTask::getId)
            .containsExactly(
                taskEntities.reversed().stream().limit(pageSize).map(TaskEntity::getId).toArray(String[]::new)
            );

        assertThat(retrievedTasks)
            .allSatisfy(task -> {
                assertThat(task.getProcessVariables()).hasSizeLessThanOrEqualTo(processVariableKeys.size());
                assertThat(task.getProcessVariables())
                    .allSatisfy(variable ->
                        assertThat(processVariableKeys)
                            .anyMatch(vk ->
                                vk.equals(processInstanceEntity.getProcessDefinitionKey() + "/" + variable.getName())
                            )
                    );
            });
    }

    @Test
    public void should_return_PaginatedTasks_WithProcessVariables() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> taskEntities = createTasks(processInstanceEntity, 10);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<String> processVariableKeys = variables
            .stream()
            .map(v -> processInstanceEntity.getProcessDefinitionKey() + "/" + v.getName())
            .toList();

        Pageable pageable = PageRequest.of(0, 3, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys
        );

        assertThat(response.getContent()).hasSize(pageable.getPageSize());
        assertThat(response.getPreviousLink()).isEmpty();
        assertThat(response.getNextLink()).isPresent();

        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .extracting(QueryCloudTask::getId)
            .containsExactly(
                taskEntities
                    .reversed()
                    .stream()
                    .limit(pageable.getPageSize())
                    .map(TaskEntity::getId)
                    .toArray(String[]::new)
            );

        pageable = PageRequest.of(1, 3, Sort.by("createdDate").descending());

        response =
            taskControllerHelper.findAllWithProcessVariables(
                predicate,
                variableSearch,
                pageable,
                filters,
                processVariableKeys
            );

        assertThat(response.getContent()).hasSize(pageable.getPageSize());
        assertThat(response.getPreviousLink()).isPresent();
        assertThat(response.getNextLink()).isPresent();

        pageable = PageRequest.of(3, 3, Sort.by("createdDate").descending());

        response =
            taskControllerHelper.findAllWithProcessVariables(
                predicate,
                variableSearch,
                pageable,
                filters,
                processVariableKeys
            );

        assertThat(response.getContent()).hasSize(taskEntities.size() - pageable.getPageSize() * 3);
        assertThat(response.getPreviousLink()).isPresent();
        assertThat(response.getNextLink()).isEmpty();
    }

    @Test
    void should_returnBothTasks_whenOneTaskHasNoMatchingProcessVariablesFetchKeys() {
        ProcessInstanceEntity processInstanceEntity1 = createProcessInstance("processDefinitionKey1");
        Set<ProcessVariableEntity> variables1 = createProcessVariables(processInstanceEntity1, 5);

        ProcessInstanceEntity processInstanceEntity2 = createProcessInstance("processDefinitionKey2");
        Set<ProcessVariableEntity> variables2 = createProcessVariables(processInstanceEntity2, 7);

        TaskEntity process1Task = new TaskEntity();
        String taskId = "task_id_1";
        process1Task.setId(taskId);
        process1Task.setCreatedDate(new Date());
        process1Task.setProcessVariables(variables1);
        process1Task.setProcessInstanceId(processInstanceEntity1.getId());
        taskRepository.save(process1Task);

        TaskEntity process2Task = new TaskEntity();
        String taskId2 = "task_id_2";
        process2Task.setId(taskId2);
        process2Task.setCreatedDate(new Date());
        process2Task.setProcessVariables(variables2);
        process2Task.setProcessInstanceId(processInstanceEntity2.getId());
        taskRepository.save(process2Task);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").ascending());

        List<String> processVariableFetchKeys = variables1
            .stream()
            .limit(3)
            .map(v -> processInstanceEntity1.getProcessDefinitionKey() + "/" + v.getName())
            .toList();

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableFetchKeys
        );

        assertThat(response.getContent()).hasSize(2);
        Optional<QueryCloudTask> task1 = response
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .filter(t -> t.getId().equals(taskId))
            .findFirst();
        assertThat(task1)
            .isPresent()
            .get()
            .extracting(QueryCloudTask::getProcessVariables)
            .satisfies(pv -> assertThat(pv).hasSameSizeAs(processVariableFetchKeys));
        Optional<QueryCloudTask> task2 = response
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .filter(t -> t.getId().equals(taskId2))
            .findFirst();
        assertThat(task2)
            .isPresent()
            .get()
            .extracting(QueryCloudTask::getProcessVariables)
            .satisfies(pv -> assertThat(pv).isNullOrEmpty());
    }

    @Test
    void should_returnTask_whenItHashNoMatchingProcessVariablesFetchKeys() {
        String processDefinitionKey = "processDefinitionKey";
        ProcessInstanceEntity processInstanceEntity1 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstanceEntity2 = createProcessInstance(processDefinitionKey);
        Set<ProcessVariableEntity> variables1 = createProcessVariables(processInstanceEntity1, 2);
        createProcessVariables(processInstanceEntity2, 2);

        TaskEntity taskEntity = new TaskEntity();
        String taskId = "task_id";
        taskEntity.setId(taskId);
        taskEntity.setCreatedDate(new Date());
        TaskCandidateGroupEntity groupCand = new TaskCandidateGroupEntity(taskId, "group");
        taskEntity.setTaskCandidateGroups(Set.of(groupCand));
        TaskCandidateUserEntity usrCand = new TaskCandidateUserEntity(taskId, "user");
        taskEntity.setTaskCandidateUsers(Set.of(usrCand));
        taskEntity.setProcessVariables(variables1);
        taskEntity.setProcessInstance(processInstanceEntity1);
        taskEntity.setProcessInstanceId(processInstanceEntity1.getId());
        taskCandidateGroupRepository.save(groupCand);
        taskCandidateUserRepository.save(usrCand);
        taskRepository.save(taskEntity);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        List<String> processVariableKeys = Stream
            .of("other-variable", "another-variable")
            .map(v -> processInstanceEntity1.getProcessDefinitionKey() + "/" + v)
            .toList();

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().stream().toList().getFirst().getContent().getProcessVariables()).isEmpty();
    }

    @Test
    void should_returnOnlyStandaloneTasks_whenStandAloneFilterIsTrue() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> tasksWithProcessInstance = createTasks(processInstanceEntity, 10);
        List<TaskEntity> standaloneTasks = createStandaloneTasks();

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(true));

        int pageSize = 1000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        List<String> processVariableKeys = variables
            .stream()
            .map(v -> processInstanceEntity.getProcessDefinitionKey() + "/" + v.getName())
            .toList();

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys
        );

        assertThat(response.getContent()).hasSize(standaloneTasks.size());
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(standaloneTasks);
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .doesNotContainAnyElementsOf(tasksWithProcessInstance);
    }

    @Test
    void should_returnAllTasks_whenStandAloneFilterIsFalse() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> tasksWithProcessInstance = createTasks(processInstanceEntity, 10);
        List<TaskEntity> standaloneTasks = createStandaloneTasks();

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        int pageSize = 1000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        List<String> processVariableKeys = variables
            .stream()
            .map(v -> processInstanceEntity.getProcessDefinitionKey() + "/" + v.getName())
            .toList();

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys
        );

        assertThat(response.getContent()).hasSize(standaloneTasks.size() + tasksWithProcessInstance.size());
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(standaloneTasks);
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .containsAll(tasksWithProcessInstance);
    }

    @Test
    void should_returnOnlyRootTasks_whenRootTaskFilterIsTrue() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> rootTasks = createTasks(processInstanceEntity, 10);

        List<TaskEntity> childTasks = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            TaskEntity childTask = new TaskEntity();
            String taskId = "child" + i;
            childTask.setId(taskId);
            childTask.setCreatedDate(new Date());
            childTask.setProcessInstance(processInstanceEntity);
            childTask.setProcessInstanceId(processInstanceEntity.getId());
            childTask.setParentTaskId(rootTasks.get(i).getId());
            childTasks.add(childTask);
            taskRepository.save(childTask);
        }

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(true), new StandAloneTaskFilter(false));

        int pageSize = 1000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        List<String> processVariableKeys = variables
            .stream()
            .map(v -> processInstanceEntity.getProcessDefinitionKey() + "/" + v.getName())
            .toList();

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys
        );

        assertThat(response.getContent()).hasSize(rootTasks.size());
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(rootTasks);
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .doesNotContainAnyElementsOf(childTasks);
    }

    @Test
    void should_returnRootTasksAndChildTasks_whenRootTaskFilterIsFalse() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> rootTasks = createTasks(processInstanceEntity, 10);

        List<TaskEntity> childTasks = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            TaskEntity childTask = new TaskEntity();
            String taskId = "child" + i;
            childTask.setId(taskId);
            childTask.setCreatedDate(new Date());
            childTask.setProcessInstance(processInstanceEntity);
            childTask.setProcessInstanceId(processInstanceEntity.getId());
            childTask.setParentTaskId(rootTasks.get(i).getId());
            childTasks.add(childTask);
            taskRepository.save(childTask);
        }

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        int pageSize = 1000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        List<String> processVariableKeys = variables
            .stream()
            .map(v -> processInstanceEntity.getProcessDefinitionKey() + "/" + v.getName())
            .toList();

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys
        );

        assertThat(response.getContent()).hasSize(rootTasks.size() + childTasks.size());
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(rootTasks);
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(childTasks);
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_exactMatch() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "string-var";
        String valueToSearch = "string-value";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.STRING, valueToSearch);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.STRING, "different-string-value");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.STRING, valueToSearch);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task -> {
                assertThat(task.getProcessInstanceId()).isEqualTo(processInstance1.getId());
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && pv.getValue().equals(valueToSearch));
            });
    }

    @Test
    void should_returnTasks_filteredByTaskProcessVariable_exactMatch() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "task-var";
        String valueToSearch = "task-value";
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.STRING, valueToSearch);
        createTaskWithVariable(processInstance, varName, VariableType.STRING, "different-value");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_contains() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "string-var";
        String valueToSearch = "jaeger";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.STRING, "Eren Jaeger");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.STRING, "Frank Jaeger");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.STRING, valueToSearch);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.CONTAINS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv ->
                        pv.getName().equals(varName) &&
                        ((String) pv.getValue()).toLowerCase().contains(valueToSearch.toLowerCase())
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByTaskProcessVariable_contains() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "task-var";
        String valueToSearch = "fox";
        QueryCloudTask task1 = createTaskWithVariable(processInstance, varName, VariableType.STRING, "Gray Fox");
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.STRING, "Fox Hound");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.CONTAINS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_equals() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "int-var";
        int valueToSearch = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, valueToSearch);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, valueToSearch + 1);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, valueToSearch);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .hasSize(1)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && pv.getValue().equals(valueToSearch))
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_equals() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int valueToSearch = 42;
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, valueToSearch);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, valueToSearch + 1);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_greaterThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "int-var";
        int lowerBound = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, lowerBound + 1);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, lowerBound + 1);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .hasSize(1)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() > lowerBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int lowerBound = 42;
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound + 1);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_greaterThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "int-var";
        int lowerBound = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, lowerBound + 1);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, lowerBound + 1);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() >= lowerBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int lowerBound = 42;
        QueryCloudTask task1 = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound + 1);
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound - 1);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_lessThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "int-var";
        int upperBound = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, upperBound - 1);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, upperBound - 1);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() < upperBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int upperBound = 42;
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound - 1);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_lessThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "int-var";
        int upperBound = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, upperBound - 1);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, upperBound - 1);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() <= upperBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int upperBound = 42;
        QueryCloudTask task1 = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound - 1);
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound + 1);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_equals() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "bigdecimal-var";
        BigDecimal valueToSearch = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, valueToSearch);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.BIGDECIMAL, valueToSearch);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv ->
                        pv.getName().equals(varName) && new BigDecimal(pv.getValue().toString()).equals(valueToSearch)
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_equals() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal valueToSearch = new BigDecimal("42.42");
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, valueToSearch);
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_greaterThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.43")
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isGreaterThan(lowerBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.43")
        );
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, lowerBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_greaterThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.43")
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isGreaterThanOrEqualTo(lowerBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.43")
        );
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, lowerBound);
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, new BigDecimal("42.41"));

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_lessThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, new BigDecimal("42.41"));
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.41")
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isLessThan(upperBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.41")
        );
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, upperBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_lessThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, new BigDecimal("42.41"));
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.41")
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isLessThanOrEqualTo(upperBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.41")
        );
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, upperBound);
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_equals() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String valueToSearch = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-03T00:12:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-02T00:13:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isEqualTo(DateUtils.parseDate(valueToSearch));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_equals() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String valueToSearch = "2024-08-02";
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-02T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-03T00:12:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_greaterThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String lowerBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-03T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-03T00:12:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isAfter(DateUtils.parseDate(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String lowerBound = "2024-08-02";
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-03T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_greaterThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String lowerBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-03T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-03T00:12:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isAfterOrEqualTo(DateUtils.parseDate(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String lowerBound = "2024-08-02";
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-03T00:11:00.000+0000"
        );
        QueryCloudTask task2 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-02T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-01T00:11:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_lessThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String upperBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-01T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-01T00:11:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isBefore(DateUtils.parseDate(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String upperBound = "2024-08-02";
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-01T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_lessThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String upperBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-01T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-01T00:11:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isBeforeOrEqualTo(DateUtils.parseDate(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String upperBound = "2024-08-02";
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-01T00:11:00.000+0000"
        );
        QueryCloudTask task2 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-02T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-03T00:11:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByDateTimeProcessVariable_equals() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String valueToSearch = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:11:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()))
                            .isEqualTo(DateUtils.parseDateTime(valueToSearch));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTimeTaskVariable_equals() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";

        String valueToSearch = "2024-08-02T00:11:00.000+0000";
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.DATETIME, valueToSearch);
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_greaterThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue())).isAfter(DateUtils.parseDateTime(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+0000";
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, lowerBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_greaterThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()))
                            .isAfterOrEqualTo(DateUtils.parseDateTime(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+0000";
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+0000"
        );
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.DATETIME, lowerBound);
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_lessThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()))
                            .isBefore(DateUtils.parseDateTime(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+0000";

        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, upperBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_lessThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()))
                            .isBeforeOrEqualTo(DateUtils.parseDateTime(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+0000";

        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+0000"
        );
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.DATETIME, upperBound);
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    private void createProcessVariableAndTask(
        ProcessInstanceEntity processInstance,
        String name,
        VariableType variableType,
        Object value
    ) {
        ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
        processVariableEntity.setName(name);
        processVariableEntity.setType(variableType.name().toLowerCase());
        switch (variableType) {
            case STRING, BIGDECIMAL, DATE, DATETIME -> processVariableEntity.setValue(String.valueOf(value));
            case INTEGER -> processVariableEntity.setValue((Integer) value);
            case BOOLEAN -> processVariableEntity.setValue((Boolean) value);
        }
        processVariableEntity.setProcessInstanceId(processInstance.getId());
        processVariableEntity.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
        variableRepository.save(processVariableEntity);
        processInstance.setVariables(Set.of(processVariableEntity));
        processInstanceRepository.save(processInstance);
        createTasks(processInstance, 1);
    }

    @NotNull
    private static TaskSearchRequest buildTaskSearchRequestWithProcessVariableFilter(VariableFilter variableFilter) {
        Set<VariableFilter> filters = Set.of(variableFilter);
        Set<ProcessVariableKey> processVariableKeys = Set.of(
            new ProcessVariableKey(variableFilter.processDefinitionKey(), variableFilter.name())
        );
        return new TaskSearchRequest(
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            filters,
            processVariableKeys
        );
    }

    @NotNull
    private static TaskSearchRequest buildTaskSearchRequestWithTaskVariableFilter(VariableFilter variableFilter) {
        return new TaskSearchRequest(
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Set.of(variableFilter),
            null,
            null
        );
    }

    @NotNull
    private List<TaskEntity> createStandaloneTasks() {
        List<TaskEntity> taskEntities = new ArrayList<>();

        LocalDateTime start = LocalDateTime.fromDateFields(new Date());
        for (int i = 0; i < 10; i++) {
            TaskEntity taskEntity = new TaskEntity();
            String taskId = "standalone" + i;
            taskEntity.setId(taskId);
            taskEntity.setCreatedDate(start.plusSeconds(i).toDate());
            taskEntities.add(taskEntity);
            taskRepository.save(taskEntity);
        }
        return taskEntities;
    }

    @NotNull
    private Set<ProcessVariableEntity> createProcessVariables(
        ProcessInstanceEntity processInstanceEntity,
        int numberOfVariables
    ) {
        Set<ProcessVariableEntity> variables = new HashSet<>();

        for (int i = 0; i < numberOfVariables; i++) {
            ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
            processVariableEntity.setName("name" + i);
            processVariableEntity.setValue("value" + i);
            processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
            processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
            processVariableEntity.setProcessInstance(processInstanceEntity);
            variables.add(processVariableEntity);
        }
        variableRepository.saveAll(variables);
        processInstanceEntity.setVariables(variables);
        processInstanceRepository.save(processInstanceEntity);
        return variables;
    }

    @NotNull
    private List<TaskEntity> createTasks(ProcessInstanceEntity processInstanceEntity, int numberOfTasks) {
        List<TaskEntity> taskEntities = new ArrayList<>();

        LocalDateTime start = LocalDateTime.fromDateFields(new Date());
        for (int i = 0; i < numberOfTasks; i++) {
            TaskEntity taskEntity = new TaskEntity();
            String taskId = UUID.randomUUID().toString();
            taskEntity.setId(taskId);
            taskEntity.setCreatedDate(start.plusSeconds(i).toDate());
            TaskCandidateGroupEntity groupCand = new TaskCandidateGroupEntity(taskId, "group" + i);
            taskEntity.setTaskCandidateGroups(Set.of(groupCand));
            TaskCandidateUserEntity usrCand = new TaskCandidateUserEntity(taskId, "user" + i);
            taskEntity.setTaskCandidateUsers(Set.of(usrCand));
            taskEntity.setProcessVariables(processInstanceEntity.getVariables());
            taskEntity.setProcessInstance(processInstanceEntity);
            taskEntity.setProcessInstanceId(processInstanceEntity.getId());
            taskEntities.add(taskEntity);
            taskCandidateGroupRepository.save(groupCand);
            taskCandidateUserRepository.save(usrCand);
            taskRepository.save(taskEntity);
        }
        processInstanceEntity.setTasks(Set.copyOf(taskEntities));
        processInstanceRepository.save(processInstanceEntity);
        return taskEntities;
    }

    @NotNull
    private Set<ProcessVariableEntity> createProcessVariables(ProcessInstanceEntity processInstanceEntity) {
        return createProcessVariables(processInstanceEntity, 8);
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance(String processDefinitionKey) {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId(UUID.randomUUID().toString());
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionName("test");
        processInstanceEntity.setProcessDefinitionKey(processDefinitionKey);
        processInstanceEntity.setServiceName("test");
        processInstanceRepository.save(processInstanceEntity);
        return processInstanceEntity;
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance() {
        return createProcessInstance("processDefinitionKey");
    }

    private TaskEntity createTaskWithVariable(
        ProcessInstanceEntity processInstance,
        String varName,
        VariableType variableType,
        Object value
    ) {
        TaskEntity taskEntity = new TaskEntity();
        String taskId = UUID.randomUUID().toString();
        taskEntity.setId(taskId);
        taskEntity.setCreatedDate(new Date());
        taskEntity.setProcessInstanceId(processInstance.getId());

        TaskVariableEntity taskVariableEntity = new TaskVariableEntity();
        taskVariableEntity.setName(varName);
        taskVariableEntity.setType(variableType.name().toLowerCase());
        switch (variableType) {
            case STRING, BIGDECIMAL, DATE, DATETIME -> taskVariableEntity.setValue(String.valueOf(value));
            case INTEGER -> taskVariableEntity.setValue((Integer) value);
            case BOOLEAN -> taskVariableEntity.setValue((Boolean) value);
        }
        taskVariableEntity.setTaskId(taskId);
        taskVariableRepository.save(taskVariableEntity);
        taskEntity.setVariables(Set.of(taskVariableEntity));
        taskRepository.save(taskEntity);

        return taskEntity;
    }
}
