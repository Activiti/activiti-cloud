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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.activiti.cloud.services.query.rest.predicate.RootTasksFilter;
import org.activiti.cloud.services.query.rest.predicate.StandAloneTaskFilter;
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
import org.springframework.data.util.Pair;
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

    private static final Map<String, Pair<VariableType, Object>> processVariables = Stream
        .of(
            Pair.of("string-var-1", Pair.of(VariableType.STRING, (Object) "string1")),
            Pair.of("string-var-2", Pair.of(VariableType.STRING, (Object) "string2")),
            Pair.of("integer-var-1", Pair.of(VariableType.INTEGER, (Object) 10)),
            Pair.of("integer-var-2", Pair.of(VariableType.INTEGER, (Object) 20)),
            Pair.of("integer-var-3", Pair.of(VariableType.INTEGER, (Object) 30)),
            Pair.of("bigdecimal-var-1", Pair.of(VariableType.BIGDECIMAL, (Object) 10.0)),
            Pair.of("bigdecimal-var-2", Pair.of(VariableType.BIGDECIMAL, (Object) 20.0)),
            Pair.of("bigdecimal-var-3", Pair.of(VariableType.BIGDECIMAL, (Object) 30.0)),
            Pair.of("boolean-var-1", Pair.of(VariableType.BOOLEAN, (Object) true)),
            Pair.of("boolean-var-2", Pair.of(VariableType.BOOLEAN, (Object) false)),
            Pair.of("datetime-var-1", Pair.of(VariableType.DATETIME, (Object) "2024-08-01T00:12:00.000+0000")),
            Pair.of("datetime-var-2", Pair.of(VariableType.DATETIME, (Object) "2024-08-01T00:12:30.000+0000")),
            Pair.of("datetime-var-3", Pair.of(VariableType.DATETIME, (Object) "2024-08-01T00:13:00.000+0000")),
            Pair.of("date-var-1", Pair.of(VariableType.DATE, (Object) "2024-08-01T00:00:00.000+0000")),
            Pair.of("date-var-2", Pair.of(VariableType.DATE, (Object) "2024-08-02T00:00:00.000+0000")),
            Pair.of("date-var-3", Pair.of(VariableType.DATE, (Object) "2024-08-03T00:00:00.000+0000"))
        )
        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

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
    void should_returnTask_whenItHashNoMatchingProcessVariablesFetchKeys() {
        ProcessInstanceEntity processInstanceEntity1 = createProcessInstance("processDefinitionKey1");
        Set<ProcessVariableEntity> variables1 = createProcessVariables(processInstanceEntity1);

        ProcessInstanceEntity processInstanceEntity2 = createProcessInstance("processDefinitionKey2");
        Set<ProcessVariableEntity> variables2 = createProcessVariables(processInstanceEntity2);

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
            .satisfies(pv -> assertThat(pv).hasSize(variables1.size()));
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
    void should_returnBothTasks_whenOneTaskHasNoMatchingProcessVariablesFetchKeys() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);

        Set<ProcessVariableEntity> otherVariables = variables.stream().skip(4).collect(Collectors.toSet());
        variables.removeAll(otherVariables);

        TaskEntity taskWithoutVariables = new TaskEntity();
        String taskId = "task_id";
        taskWithoutVariables.setId(taskId);
        taskWithoutVariables.setCreatedDate(new Date());
        taskWithoutVariables.setProcessVariables(variables);
        taskWithoutVariables.setProcessInstance(processInstanceEntity);
        taskWithoutVariables.setProcessInstanceId(processInstanceEntity.getId());
        taskRepository.save(taskWithoutVariables);

        TaskEntity taskWithVariables = new TaskEntity();
        taskWithVariables.setId("task_id_2");
        taskWithVariables.setCreatedDate(new Date());
        taskWithVariables.setProcessVariables(otherVariables);
        taskWithVariables.setProcessInstance(processInstanceEntity);
        taskWithVariables.setProcessInstanceId(processInstanceEntity.getId());
        taskRepository.save(taskWithVariables);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").ascending());

        List<String> processVariableFetchKeys = otherVariables
            .stream()
            .map(v -> processInstanceEntity.getProcessDefinitionKey() + "/" + v.getName())
            .toList();

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableFetchKeys
        );

        assertThat(response.getContent()).hasSize(2);
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
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .allSatisfy(task -> assertThat(standaloneTasks).extracting(TaskEntity::getId).contains(task.getId()));
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .noneSatisfy(task ->
                assertThat(tasksWithProcessInstance).extracting(TaskEntity::getId).contains(task.getId())
            );
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
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(standaloneTasks);
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(tasksWithProcessInstance);
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
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .allSatisfy(task -> assertThat(rootTasks).extracting(TaskEntity::getId).contains(task.getId()));
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .noneSatisfy(task -> assertThat(childTasks).extracting(TaskEntity::getId).contains(task.getId()));
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
        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .allSatisfy(task ->
                assertThat(Stream.of(childTasks, rootTasks).flatMap(List::stream))
                    .extracting(TaskEntity::getId)
                    .contains(task.getId())
            );
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_exactMatch() {
        String processDefinitionKey = "processDefinitionKey";
        String anotherProcessDefinitionKey = "anotherProcessDefinitionKey";
        List<ProcessInstanceEntity> processInstances1 = createProcessInstancesAndVariablesAndTasks(
            processDefinitionKey
        );
        List<ProcessInstanceEntity> processInstances2 = createProcessInstancesAndVariablesAndTasks(
            processDefinitionKey
        );
        List<ProcessInstanceEntity> processInstances3 = createProcessInstancesAndVariablesAndTasks(
            anotherProcessDefinitionKey
        );

        Map.Entry<String, Pair<VariableType, Object>> variableToFilter = processVariables
            .entrySet()
            .stream()
            .filter(e -> e.getValue().getFirst() == VariableType.STRING)
            .findAny()
            .get();
        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            variableToFilter.getKey(),
            VariableType.STRING,
            (String) variableToFilter.getValue().getSecond(),
            FilterOperator.EQUALS
        );
        TaskSearchRequest taskSearchRequest = buildTaskSearchRequest(variableFilter);

        Set<TaskEntity> expectedTasks = Stream
            .of(processInstances1, processInstances2, processInstances3)
            .flatMap(List::stream)
            .map(ProcessInstanceEntity::getTasks)
            .flatMap(Set::stream)
            .filter(t ->
                t
                    .getProcessVariables()
                    .stream()
                    .anyMatch(v ->
                        v.getProcessDefinitionKey().equals(processDefinitionKey) &&
                        v.getName().equals(variableToFilter.getKey()) &&
                        v.getValue().equals(variableToFilter.getValue().getSecond())
                    )
            )
            .collect(Collectors.toSet());

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 10000))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).hasSize(expectedTasks.size());
    }

    @NotNull
    private static TaskSearchRequest buildTaskSearchRequest(VariableFilter variableFilter) {
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
    private Set<ProcessVariableEntity> createProcessVariables(ProcessInstanceEntity processInstanceEntity) {
        Set<ProcessVariableEntity> variables = new HashSet<>();

        for (int i = 0; i < 8; i++) {
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
    private ProcessInstanceEntity createProcessInstance() {
        return createProcessInstance(UUID.randomUUID().toString());
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
    private List<ProcessInstanceEntity> createProcessInstancesAndVariablesAndTasks(String processDefinitionKey) {
        List<ProcessInstanceEntity> processInstanceEntities = new ArrayList<>();
        for (Map.Entry<String, Pair<VariableType, Object>> entry : processVariables.entrySet()) {
            ProcessInstanceEntity processInstanceEntity = createProcessInstance(processDefinitionKey);
            processInstanceEntities.add(processInstanceEntity);
            ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
            processVariableEntity.setName(entry.getKey());
            processVariableEntity.setValue(entry.getValue().getSecond().toString());
            processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
            processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
            variableRepository.save(processVariableEntity);
            processInstanceEntity.setVariables(Set.of(processVariableEntity));
            processInstanceRepository.save(processInstanceEntity);
            createTasks(processInstanceEntity, 2);
        }
        return processInstanceEntities;
    }
}
