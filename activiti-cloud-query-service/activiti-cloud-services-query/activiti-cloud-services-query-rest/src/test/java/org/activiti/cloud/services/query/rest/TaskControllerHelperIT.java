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

import com.mxgraph.canvas.mxGraphicsCanvas2D;
import com.querydsl.core.types.Predicate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.activiti.cloud.services.query.app.repository.*;
import org.activiti.cloud.services.query.model.*;
import org.activiti.cloud.services.query.rest.dto.TaskDto;
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

    @Autowired
    private ProcessVariablesPivotRepository processVariablesPivotRepository;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        taskVariableRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
        processVariablesPivotRepository.deleteAll();
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

        PagedModel<EntityModel<TaskDto>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Collections.emptySet(),
            processVariableKeys
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .collect(Collectors.toSet())
        );

        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .extracting(TaskDto::getId)
            .containsExactly(
                taskEntities.reversed().stream().limit(pageSize).map(TaskEntity::getId).toArray(String[]::new)
            );

        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .containsExactlyInAnyOrderEntriesOf(
                        variables
                            .stream()
                            .filter(v -> processVariableKeys.contains(uniqueVarName(v, processInstanceEntity)))
                            .collect(Collectors.toMap(ProcessVariableEntity::getName, ProcessVariableEntity::getValue))
                    )
            );
    }

    @Test
    void should_returnTasks_withProcessVariablesByKeysAndFilters() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);
        List<TaskEntity> taskEntities = createTasks(variables, processInstanceEntity);

        ProcessInstanceEntity processInstanceEntity1 = createProcessInstance();
        Set<ProcessVariableEntity> variables1 = createProcessVariables(processInstanceEntity1, "test");
        List<TaskEntity> taskEntities1 = createTasks(variables1, processInstanceEntity1);

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

        ProcessVariableValueFilter filter = new ProcessVariableValueFilter(
            processInstanceEntity.getProcessDefinitionKey(),
            "name6",
            "string",
            "value6",
            ProcessVariableFilterType.EQUALS
        );
        PagedModel<EntityModel<TaskDto>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Set.of(filter),
            processVariableKeys
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .collect(Collectors.toSet())
        );

        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .extracting(TaskDto::getId)
            .containsExactly(
                taskEntities.reversed().stream().limit(pageSize).map(TaskEntity::getId).toArray(String[]::new)
            );

        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .containsExactlyInAnyOrderEntriesOf(
                        variables
                            .stream()
                            .filter(v -> processVariableKeys.contains(uniqueVarName(v, processInstanceEntity)))
                            .collect(Collectors.toMap(ProcessVariableEntity::getName, ProcessVariableEntity::getValue))
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
            .map(v -> uniqueVarName(v, processInstanceEntity))
            .toList();

        Pageable pageable = PageRequest.of(0, 3, Sort.by("createdDate").descending());

        PagedModel<EntityModel<TaskDto>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Collections.emptySet(),
            processVariableKeys
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .collect(Collectors.toSet())
        );

        assertThat(response.getContent()).hasSize(pageable.getPageSize());
        assertThat(response.getPreviousLink()).isEmpty();
        assertThat(response.getNextLink()).isPresent();

        assertThat(response.getContent().stream().map(EntityModel::getContent).toList())
            .extracting(TaskDto::getId)
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
                Collections.emptySet(),
                processVariableKeys
                    .stream()
                    .map(k -> k.split("/"))
                    .map(s -> new ProcessVariableKey(s[0], s[1]))
                    .collect(Collectors.toSet())
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
                Collections.emptySet(),
                processVariableKeys
                    .stream()
                    .map(k -> k.split("/"))
                    .map(s -> new ProcessVariableKey(s[0], s[1]))
                    .collect(Collectors.toSet())
            );

        assertThat(response.getContent()).hasSize(taskEntities.size() - pageable.getPageSize() * 3);
        assertThat(response.getPreviousLink()).isPresent();
        assertThat(response.getNextLink()).isEmpty();
    }

    @Test
    void should_returnTask_whenItHashNoMatchingProcessVariablesFetchKeys() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity);

        TaskEntity taskWithoutVariables = new TaskEntity();
        String taskId = "task_id";
        taskWithoutVariables.setId(taskId);
        taskWithoutVariables.setCreatedDate(new Date());
        taskWithoutVariables.setProcessVariables(Collections.emptySet());
        taskWithoutVariables.setProcessInstance(processInstanceEntity);
        taskWithoutVariables.setProcessInstanceId(processInstanceEntity.getId());
        taskRepository.save(taskWithoutVariables);

        TaskEntity taskWithVariables = new TaskEntity();
        taskWithVariables.setId("task_id_2");
        taskWithVariables.setCreatedDate(new Date());
        taskWithVariables.setProcessVariables(variables);
        taskWithVariables.setProcessInstance(processInstanceEntity);
        taskWithVariables.setProcessInstanceId(processInstanceEntity.getId());
        taskRepository.save(taskWithVariables);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").ascending());

        List<String> processVariableFetchKeys = variables
            .stream()
            .map(v -> uniqueVarName(v, processInstanceEntity))
            .toList();

        PagedModel<EntityModel<TaskDto>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Collections.emptySet(),
            processVariableFetchKeys
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .collect(Collectors.toSet())
        );

        assertThat(response.getContent()).hasSize(2);

        assertThat(response.getContent().stream().toList().getFirst().getContent().getProcessVariables()).isEmpty();
        assertThat(response.getContent().stream().toList().get(1).getContent().getProcessVariables())
            .hasSize(processVariableFetchKeys.size());
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
            .map(v -> uniqueVarName(v, processInstanceEntity))
            .toList();

        PagedModel<EntityModel<TaskDto>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Collections.emptySet(),
            processVariableFetchKeys
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .collect(Collectors.toSet())
        );

        assertThat(response.getContent()).hasSize(2);
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
            .map(v -> uniqueVarName(v, processInstanceEntity))
            .toList();

        PagedModel<EntityModel<TaskDto>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Collections.emptySet(),
            processVariableKeys
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .collect(Collectors.toSet())
        );

        assertThat(response.getContent()).hasSize(standaloneTasks.size());
        //TODO fix test
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(standaloneTasks);
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).doesNotContainAnyElementsOf(tasksWithProcessInstance);
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
            .map(v -> uniqueVarName(v, processInstanceEntity))
            .toList();

        PagedModel<EntityModel<TaskDto>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Collections.emptySet(),
            processVariableKeys
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .collect(Collectors.toSet())
        );

        assertThat(response.getContent()).hasSize(standaloneTasks.size() + tasksWithProcessInstance.size());
        //TODO fix test
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(standaloneTasks);
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(tasksWithProcessInstance);
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
            .map(v -> uniqueVarName(v, processInstanceEntity))
            .toList();

        PagedModel<EntityModel<TaskDto>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Collections.emptySet(),
            processVariableKeys
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .collect(Collectors.toSet())
        );

        assertThat(response.getContent()).hasSize(rootTasks.size());
        //TODO fix test
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(rootTasks);
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).doesNotContainAnyElementsOf(childTasks);
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
            .map(v -> uniqueVarName(v, processInstanceEntity))
            .toList();

        PagedModel<EntityModel<TaskDto>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Collections.emptySet(),
            processVariableKeys
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .collect(Collectors.toSet())
        );

        assertThat(response.getContent()).hasSize(rootTasks.size() + childTasks.size());
        //TODO fix test
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(rootTasks);
        //assertThat(response.getContent().stream().map(EntityModel::getContent).toList()).containsAll(childTasks);
    }

    //TODO should return empty list when process variable value filter has no matches

    @NotNull
    private List<TaskEntity> createTasks(
        Set<ProcessVariableEntity> variables,
        ProcessInstanceEntity processInstanceEntity
    ) {
        List<TaskEntity> taskEntities = new ArrayList<>();

        LocalDateTime start = LocalDateTime.fromDateFields(new Date());
        for (int i = 0; i < 10; i++) {
            TaskEntity taskEntity = new TaskEntity();
            String taskId = UUID.randomUUID().toString();
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

    private Set<ProcessVariableEntity> createProcessVariables(ProcessInstanceEntity processInstanceEntity) {
        return createProcessVariables(processInstanceEntity, "");
    }

    @NotNull
    private Set<ProcessVariableEntity> createProcessVariables(ProcessInstanceEntity processInstanceEntity, String var) {
        Set<ProcessVariableEntity> variables = new HashSet<>();
        Map<String, Object> map = new HashMap<>();

        for (int i = 0; i < 8; i++) {
            ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
            processVariableEntity.setName("name" + i);
            processVariableEntity.setValue("value" + i + var);
            processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
            processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
            processVariableEntity.setProcessInstance(processInstanceEntity);
            variables.add(processVariableEntity);
            map.put(uniqueVarName(processVariableEntity, processInstanceEntity), processVariableEntity.getValue());
        }
        variableRepository.saveAll(variables);
        processInstanceEntity.setVariables(variables);
        processInstanceRepository.save(processInstanceEntity);

        ProcessVariablesPivotEntity pivot = new ProcessVariablesPivotEntity();
        pivot.setValues(map);
        pivot.setProcessInstanceId(processInstanceEntity.getId());

        processVariablesPivotRepository.save(pivot);

        return variables;
    }

    private ProcessInstanceEntity createProcessInstance() {
        return createProcessInstance("processDefinitionKey");
    }

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

    private static String uniqueVarName(ProcessVariableEntity v, ProcessInstanceEntity processInstanceEntity) {
        return processInstanceEntity.getProcessDefinitionKey() + "/" + v.getName();
    }
}
