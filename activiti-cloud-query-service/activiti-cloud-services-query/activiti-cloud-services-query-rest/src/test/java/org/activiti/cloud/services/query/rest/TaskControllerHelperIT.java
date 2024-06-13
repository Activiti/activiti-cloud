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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.activiti.cloud.services.query.rest.predicate.RootTasksFilter;
import org.activiti.cloud.services.query.rest.predicate.StandAloneTaskFilter;
import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "logging.level.org.hibernate.collection.spi=warn",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true",
    }
)
@Testcontainers
@EnableAutoConfiguration
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
    public void should_returnTasks_filteredByProcessVariableExactQuery() {
        ProcessInstanceEntity process1 = createProcessInstance();
        ProcessInstanceEntity process2 = createProcessInstance();
        Set<ProcessVariableEntity> variables1 = createProcessVariables(process1);
        Set<ProcessVariableEntity> variables2 = createProcessVariables(process2);
        List<TaskEntity> tasks1 = createTasks(variables1, process1);
        List<TaskEntity> tasks2 = createTasks(variables2, process2);

        final String varName = "var-to-search";
        final String varValue = "value-to-search";
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(varName);
        processVar1.setValue(varValue);
        processVar1.setProcessInstanceId(process1.getId());
        processVar1.setProcessDefinitionKey(process1.getProcessDefinitionKey());
        processVar1.setProcessInstance(process1);
        variableRepository.save(processVar1);

        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(varName);
        processVar2.setValue(varValue);
        processVar2.setProcessInstanceId(process2.getId());
        processVar2.setProcessDefinitionKey(process2.getProcessDefinitionKey());
        processVar2.setProcessInstance(process2);
        variableRepository.save(processVar2);

        TaskEntity taskFromProcess1 = tasks1.getLast();
        taskFromProcess1.setProcessVariables(Set.of(processVar1));
        TaskEntity taskFromProcess2 = tasks2.getLast();
        taskFromProcess2.setProcessVariables(Set.of(processVar2));
        taskRepository.save(taskFromProcess1);
        taskRepository.save(taskFromProcess2);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<String> processVariableKeys = List.of(process1.getProcessDefinitionKey() + "/" + varName);

        Map<String, Object> processVariableFilters = Map.of(
            process1.getProcessDefinitionKey() + "/" + varName,
            varValue
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys,
            processVariableFilters
        );

        assertThat(response.getContent().size()).isEqualTo(2);
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks).containsExactlyInAnyOrder(taskFromProcess1, taskFromProcess2);
        assertThat(retrievedTasks)
            .allSatisfy(task -> {
                assertThat(task.getProcessVariables()).extracting("name").anyMatch("var-to-search"::equals);
                assertThat(task.getProcessVariables()).extracting("value").anyMatch("value-to-search"::equals);
            });
    }

    @Test
    void should_returnTasks_filteredByMultipleProcessVariableExactQuery() {
        ProcessInstanceEntity process1 = createProcessInstance();
        ProcessInstanceEntity process2 = createProcessInstance();
        Set<ProcessVariableEntity> variables1 = createProcessVariables(process1);
        Set<ProcessVariableEntity> variables2 = createProcessVariables(process2);
        List<TaskEntity> tasks1 = createTasks(variables1, process1);
        List<TaskEntity> tasks2 = createTasks(variables2, process2);

        final String var1name = "var-to-search";
        final String var1value = "value-to-search";
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(var1name);
        processVar1.setValue(var1value);
        processVar1.setProcessInstanceId(process1.getId());
        processVar1.setProcessDefinitionKey(process1.getProcessDefinitionKey());
        processVar1.setProcessInstance(process1);
        variableRepository.save(processVar1);

        final String var2name = "var-to-search-2";
        final String var2value = "value-to-search-2";
        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(var2name);
        processVar2.setValue(var2value);
        processVar2.setProcessInstanceId(process2.getId());
        processVar2.setProcessDefinitionKey(process2.getProcessDefinitionKey());
        processVar2.setProcessInstance(process2);
        variableRepository.save(processVar2);

        TaskEntity taskFromProcess1 = tasks1.getLast();
        taskFromProcess1.setProcessVariables(Set.of(processVar1));
        TaskEntity taskFromProcess2 = tasks2.getLast();
        taskFromProcess2.setProcessVariables(Set.of(processVar2));
        taskRepository.save(taskFromProcess1);
        taskRepository.save(taskFromProcess2);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<String> processVariableKeys = List.of(
            process1.getProcessDefinitionKey() + "/" + var1name,
            process2.getProcessDefinitionKey() + "/" + var2name
        );

        Map<String, Object> processVariableFilters = Map.of(
            process1.getProcessDefinitionKey() + "/" + var1name,
            var1value,
            process1.getProcessDefinitionKey() + "/" + var2name,
            var2value
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys,
            processVariableFilters
        );

        assertThat(response.getContent().size()).isEqualTo(2);
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks).containsExactlyInAnyOrder(taskFromProcess1, taskFromProcess2);
        assertThat(retrievedTasks)
            .satisfiesExactlyInAnyOrder(
                task -> {
                    assertThat(task.getProcessVariables()).extracting("name").anyMatch("var-to-search"::equals);
                    assertThat(task.getProcessVariables()).extracting("value").anyMatch("value-to-search"::equals);
                },
                task -> {
                    assertThat(task.getProcessVariables()).extracting("name").anyMatch("var-to-search-2"::equals);
                    assertThat(task.getProcessVariables()).extracting("value").anyMatch("value-to-search-2"::equals);
                }
            );
    }

    @Test
    public void should_returnTasks_filteredByMultipleProcessVariableExactQuery_whenFilteredVariableIsNotToBeFetched() {
        ProcessInstanceEntity process1 = createProcessInstance();
        ProcessInstanceEntity process2 = createProcessInstance();
        Set<ProcessVariableEntity> variables1 = createProcessVariables(process1);
        Set<ProcessVariableEntity> variables2 = createProcessVariables(process2);
        List<TaskEntity> tasks1 = createTasks(variables1, process1);
        List<TaskEntity> tasks2 = createTasks(variables2, process2);

        final String var1name = "var-to-search";
        final String var1value = "value-to-search";
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(var1name);
        processVar1.setValue(var1value);
        processVar1.setProcessInstanceId(process1.getId());
        processVar1.setProcessDefinitionKey(process1.getProcessDefinitionKey());
        processVar1.setProcessInstance(process1);
        variableRepository.save(processVar1);

        final String var2name = "var-to-search-2";
        final String var2value = "value-to-search-2";
        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(var2name);
        processVar2.setValue(var2value);
        processVar2.setProcessInstanceId(process2.getId());
        processVar2.setProcessDefinitionKey(process2.getProcessDefinitionKey());
        processVar2.setProcessInstance(process2);
        variableRepository.save(processVar2);

        TaskEntity taskFromProcess1 = tasks1.getLast();
        taskFromProcess1.setProcessVariables(Set.of(processVar1));
        TaskEntity taskFromProcess2 = tasks2.getLast();
        taskFromProcess2.setProcessVariables(Set.of(processVar2));
        taskRepository.save(taskFromProcess1);
        taskRepository.save(taskFromProcess2);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<String> processVariableKeys = Collections.emptyList();

        Map<String, Object> processVariableFilters = Map.of(
            process1.getProcessDefinitionKey() + "/" + var1name,
            var1value,
            process1.getProcessDefinitionKey() + "/" + var2name,
            var2value
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys,
            processVariableFilters
        );

        assertThat(response.getContent().size()).isEqualTo(2);
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks).containsExactlyInAnyOrder(taskFromProcess1, taskFromProcess2);
        assertThat(retrievedTasks)
            .satisfiesExactlyInAnyOrder(
                task -> {
                    assertThat(task.getProcessVariables()).extracting("name").anyMatch("var-to-search"::equals);
                    assertThat(task.getProcessVariables()).extracting("value").anyMatch("value-to-search"::equals);
                },
                task -> {
                    assertThat(task.getProcessVariables()).extracting("name").anyMatch("var-to-search-2"::equals);
                    assertThat(task.getProcessVariables()).extracting("value").anyMatch("value-to-search-2"::equals);
                }
            );
    }

    @Test
    void should_returnTasks_withProcessVariablesByKeys() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> taskEntities = createTasks(variables, processInstanceEntity);

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

        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .extracting(QueryCloudTask::getId)
            .containsExactly(
                taskEntities.reversed().stream().limit(pageSize).map(TaskEntity::getId).toArray(String[]::new)
            );

        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .extracting("name")
                    .containsExactlyInAnyOrder(
                        IntStream.range(0, variables.size()).filter(i -> i % 2 == 0).mapToObj(i -> "name" + i).toArray()
                    )
            );
    }

    @Test
    public void should_return_PaginatedTasks_WithProcessVariables() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> taskEntities = createTasks(variables, processInstanceEntity);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<String> processVariableKeys = variables
            .stream()
            .map(v -> processInstanceEntity.getProcessDefinitionKey() + "/" + v.getName())
            .toList();

        Pageable pageable = PageRequest.of(0, 30, Sort.by("createdDate").descending());

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

        pageable = PageRequest.of(1, 30, Sort.by("createdDate").descending());

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

        pageable = PageRequest.of(3, 30, Sort.by("createdDate").descending());

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
    void should_returnTask_evenIfItHashNoMatchingProcessVariables() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);

        TaskEntity taskEntity = new TaskEntity();
        String taskId = "task_id";
        taskEntity.setId(taskId);
        taskEntity.setCreatedDate(new Date());
        TaskCandidateGroupEntity groupCand = new TaskCandidateGroupEntity(taskId, "group");
        taskEntity.setTaskCandidateGroups(Set.of(groupCand));
        TaskCandidateUserEntity usrCand = new TaskCandidateUserEntity(taskId, "user");
        taskEntity.setTaskCandidateUsers(Set.of(usrCand));
        taskEntity.setProcessVariables(Collections.emptySet());
        taskEntity.setProcessInstance(processInstanceEntity);
        taskEntity.setProcessInstanceId(processInstanceEntity.getId());
        taskCandidateGroupRepository.save(groupCand);
        taskCandidateUserRepository.save(usrCand);
        taskRepository.save(taskEntity);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        int pageSize = 30;
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

        assertThat(response.getContent()).hasSize(1);

        assertThat(response.getContent().stream().toList().getFirst().getContent().getProcessVariables()).isEmpty();
    }

    @Test
    void should_returnOnlyStandaloneTasks_whenStandAloneFilterIsTrue() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> tasksWithProcessInstance = createTasks(variables, processInstanceEntity);
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
        List<TaskEntity> tasksWithProcessInstance = createTasks(variables, processInstanceEntity);
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
        List<TaskEntity> rootTasks = createTasks(variables, processInstanceEntity);

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
        List<TaskEntity> rootTasks = createTasks(variables, processInstanceEntity);

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

    @NotNull
    private List<TaskEntity> createTasks(
        Set<ProcessVariableEntity> variables,
        ProcessInstanceEntity processInstanceEntity
    ) {
        List<TaskEntity> taskEntities = new ArrayList<>();

        LocalDateTime start = LocalDateTime.fromDateFields(new Date());
        for (int i = 0; i < 100; i++) {
            TaskEntity taskEntity = new TaskEntity();
            String taskId = processInstanceEntity.getId() + "-task-" + i;
            taskEntity.setId(taskId);
            taskEntity.setCreatedDate(start.plusSeconds(i).toDate());
            TaskCandidateGroupEntity groupCand = new TaskCandidateGroupEntity(taskId, "group" + i);
            taskEntity.setTaskCandidateGroups(Set.of(groupCand));
            TaskCandidateUserEntity usrCand = new TaskCandidateUserEntity(taskId, "user" + i);
            taskEntity.setTaskCandidateUsers(Set.of(usrCand));
            taskEntity.setProcessVariables(variables);
            taskEntity.setProcessInstance(processInstanceEntity);
            taskEntity.setProcessInstanceId(processInstanceEntity.getId());
            taskEntities.add(taskEntity);
            taskCandidateGroupRepository.save(groupCand);
            taskCandidateUserRepository.save(usrCand);
            taskRepository.save(taskEntity);
        }
        return taskEntities;
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
    private ProcessInstanceEntity createProcessInstance() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId(UUID.randomUUID().toString());
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionName("test");
        processInstanceEntity.setProcessDefinitionKey("processDefinitionKey");
        processInstanceEntity.setServiceName("test");
        processInstanceRepository.save(processInstanceEntity);
        return processInstanceEntity;
    }
}
