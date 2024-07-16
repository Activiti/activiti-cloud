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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableFilterType;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.ProcessVariableValueFilter;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.dto.TaskDto;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = { QueryRestTestApplication.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true",
        "logging.level.org.hibernate.collection.spi=warn",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
        "spring.jpa.properties.hibernate.cache.use_query_cache=false",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
public class TaskControllerHelperBenchmarkTest {

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

    @PersistenceContext
    private EntityManager entityManager;

    private static final List<Integer[]> originalApproachResults = new ArrayList<>();
    private static final List<Integer[]> approach1Results = new ArrayList<>();
    private static final List<Integer[]> approach2Results = new ArrayList<>();

    private static final int NUMBER_OF_ITERATIONS = 100;
    private static final int NUMBER_OF_PROCESS_INSTANCES = 100;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        taskVariableRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
    }

    @AfterAll
    public static void printResults() {
        printResults("ORIGINAL APPROACH", originalApproachResults);
        printResults("APPROACH 1", approach1Results);
        printResults("APPROACH 2", approach2Results);
    }

    private static void printResults(String title, List<Integer[]> results) {
        System.out.print("\n\n");
        String[] header = { "#filters", "#fetchVars", "avg time (ms)", "std dev (ms)" };
        String format = "%-13s%-13s%-13s%-13s%n";
        String separator = "--------------------------------------------------------------";
        System.out.println(title);
        System.out.println(separator);
        System.out.format(format, (Object[]) header);
        System.out.println(separator);
        results.forEach(r -> {
            System.out.format(format, (Object[]) r);
            System.out.println(separator);
        });
        System.out.print("\n\n");
    }

    private static Stream<Arguments> getTestParameters() {
        return Stream.of(
            Arguments.of(0, 0),
            Arguments.of(1, 1),
            Arguments.of(2, 1),
            Arguments.of(5, 1),
            Arguments.of(15, 1),
            Arguments.of(1, 2),
            Arguments.of(1, 5),
            Arguments.of(1, 15),
            Arguments.of(2, 2),
            Arguments.of(5, 5),
            Arguments.of(15, 15)
        );
    }

    @ParameterizedTest
    @MethodSource("getTestParameters")
    void should_returnTasks_filteredByStringProcessVariableExactQuery(
        int numOfProcessVarFilters,
        int numOfProcessVarsToFetch
    ) {
        String processDefinitionKey = "processDefinitionKey";

        List<ProcessInstanceEntity> processInstances = IntStream
            .range(0, NUMBER_OF_PROCESS_INSTANCES)
            .mapToObj(i -> {
                ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
                processInstanceEntity.setId(UUID.randomUUID().toString());
                processInstanceEntity.setName("name" + i);
                processInstanceEntity.setInitiator("initiator");
                processInstanceEntity.setProcessDefinitionName("test");
                processInstanceEntity.setProcessDefinitionKey("processDefinitionKey");
                processInstanceEntity.setServiceName("test");
                return processInstanceEntity;
            })
            .collect(Collectors.toList());

        processInstanceRepository.saveAll(processInstances);

        Map<String, String> processVariableNamesAndValues = IntStream
            .range(0, 16)
            .mapToObj(i -> Map.entry("var" + i, "value" + i))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        processInstances.forEach(processInstance -> {
            Set<ProcessVariableEntity> processVariables = processVariableNamesAndValues
                .entrySet()
                .stream()
                .map(entry -> {
                    ProcessVariableEntity processVar = new ProcessVariableEntity();
                    processVar.setName(entry.getKey());
                    processVar.setValue(entry.getValue());
                    processVar.setProcessInstanceId(processInstance.getId());
                    processVar.setProcessDefinitionKey(processDefinitionKey);
                    processVar.setProcessInstance(processInstance);
                    return processVar;
                })
                .collect(Collectors.toSet());
            variableRepository.saveAll(processVariables);
            processInstance.setVariables(processVariables);
        });

        List<TaskEntity> tasks = processInstances
            .stream()
            .map(processInstance -> {
                TaskEntity taskEntity = new TaskEntity();
                String taskId = UUID.randomUUID().toString();
                taskEntity.setId(taskId);
                taskEntity.setCreatedDate(new Date());
                TaskCandidateGroupEntity groupCand = new TaskCandidateGroupEntity(taskId, "group" + processInstance);
                taskEntity.setTaskCandidateGroups(Set.of(groupCand));
                TaskCandidateUserEntity usrCand = new TaskCandidateUserEntity(taskId, "user" + processInstance);
                taskEntity.setTaskCandidateUsers(Set.of(usrCand));
                taskEntity.setProcessVariables(processInstance.getVariables());
                taskEntity.setProcessInstance(processInstance);
                taskEntity.setProcessInstanceId(processInstance.getId());
                taskCandidateGroupRepository.save(groupCand);
                taskCandidateUserRepository.save(usrCand);
                taskRepository.save(taskEntity);
                processInstance.setTasks(Set.of(taskEntity));
                return taskEntity;
            })
            .collect(Collectors.toList());

        //change process variable values only for some processes and expect to retrieve tasks from that processes when filtering
        Collections.shuffle(processInstances);
        List<ProcessInstanceEntity> processesWithDifferentProcessVariables = processInstances.subList(0, 100);

        Set<ProcessVariableValueFilter> processVariableValueFilters = processVariableNamesAndValues
            .entrySet()
            .stream()
            .limit(numOfProcessVarFilters)
            .map(entry -> {
                String valueToSearch = UUID.randomUUID().toString();
                processesWithDifferentProcessVariables.forEach(processInstance -> {
                    ProcessVariableEntity processVariable = processInstance.getVariable(entry.getKey()).get();
                    processVariable.setValue(valueToSearch);
                    variableRepository.save(processVariable);
                });
                return new ProcessVariableValueFilter(
                    processDefinitionKey,
                    entry.getKey(),
                    "string",
                    valueToSearch,
                    ProcessVariableFilterType.EQUALS
                );
            })
            .collect(Collectors.toSet());

        List<String> expectedTasks = processesWithDifferentProcessVariables
            .stream()
            .flatMap(processInstance -> processInstance.getTasks().stream().map(TaskEntity::getId))
            .toList();

        Set<ProcessVariableKey> processVariableKeys = Stream
            .concat(
                processVariableValueFilters.stream().map(ProcessVariableValueFilter::name),
                processVariableNamesAndValues.keySet().stream()
            )
            .distinct()
            .limit(numOfProcessVarsToFetch)
            .map(name -> new ProcessVariableKey(processDefinitionKey, name))
            .collect(Collectors.toSet());

        testOriginalApproach(
            numOfProcessVarFilters,
            numOfProcessVarsToFetch,
            processVariableKeys,
            tasks,
            expectedTasks
        );
        testApproach1(
            numOfProcessVarFilters,
            numOfProcessVarsToFetch,
            processVariableValueFilters,
            processVariableKeys,
            tasks,
            expectedTasks
        );
        testApproach2(
            numOfProcessVarFilters,
            numOfProcessVarsToFetch,
            processVariableValueFilters,
            processVariableKeys,
            tasks,
            expectedTasks
        );
    }

    private void testApproach1(
        int numOfProcessVarFilters,
        int numOfProcessVarsToFetch,
        Set<ProcessVariableValueFilter> processVariableValueFilters,
        Set<ProcessVariableKey> processVariableKeys,
        List<TaskEntity> tasks,
        List<String> expectedTasks
    ) {
        Pageable pageable = PageRequest.of(0, 1000);
        PagedModel<EntityModel<TaskDto>> response = findTasksApproach1(
            processVariableValueFilters,
            processVariableKeys,
            pageable
        );
        doAssert(response, processVariableValueFilters, tasks, expectedTasks, processVariableKeys);

        StopWatch stopWatch = new StopWatch();

        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            entityManager.clear();
            stopWatch.start();
            findTasksApproach1(processVariableValueFilters, processVariableKeys, pageable);
            stopWatch.stop();
        }

        List<Long> list = Arrays.stream(stopWatch.getTaskInfo()).map(StopWatch.TaskInfo::getTimeMillis).toList();

        double averageTime = list.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);

        double standardDeviation = Math.sqrt(
            list
                .stream()
                .mapToDouble(Long::doubleValue)
                .map(value -> Math.pow(value - averageTime, 2))
                .average()
                .orElse(0.0)
        );

        approach1Results.add(
            new Integer[] {
                numOfProcessVarFilters,
                numOfProcessVarsToFetch,
                (int) averageTime,
                (int) standardDeviation,
            }
        );
    }

    private void testApproach2(
        int numOfProcessVarFilters,
        int numOfProcessVarsToFetch,
        Set<ProcessVariableValueFilter> processVariableValueFilters,
        Set<ProcessVariableKey> processVariableKeys,
        List<TaskEntity> tasks,
        List<String> expectedTasks
    ) {
        Pageable pageable = PageRequest.of(0, 1000);
        PagedModel<EntityModel<TaskDto>> response = findTasksApproach2(
            processVariableValueFilters,
            processVariableKeys,
            pageable
        );
        doAssert(response, processVariableValueFilters, tasks, expectedTasks, processVariableKeys);

        StopWatch stopWatch = new StopWatch();

        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            entityManager.clear();
            stopWatch.start();
            findTasksApproach2(processVariableValueFilters, processVariableKeys, pageable);
            stopWatch.stop();
        }

        List<Long> list = Arrays.stream(stopWatch.getTaskInfo()).map(StopWatch.TaskInfo::getTimeMillis).toList();

        double averageTime = list.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);

        double standardDeviation = Math.sqrt(
            list
                .stream()
                .mapToDouble(Long::doubleValue)
                .map(value -> Math.pow(value - averageTime, 2))
                .average()
                .orElse(0.0)
        );

        approach2Results.add(
            new Integer[] {
                numOfProcessVarFilters,
                numOfProcessVarsToFetch,
                (int) averageTime,
                (int) standardDeviation,
            }
        );
    }

    private void testOriginalApproach(
        int numOfProcessVarFilters,
        int numOfProcessVarsToFetch,
        Set<ProcessVariableKey> processVariableKeys,
        List<TaskEntity> tasks,
        List<String> expectedTasks
    ) {
        if (numOfProcessVarFilters != 0) {
            return;
        }

        Pageable pageable = PageRequest.of(0, 1000);
        PagedModel<EntityModel<TaskDto>> response = findTasksOriginal(processVariableKeys, pageable);
        doAssert(response, Collections.emptySet(), tasks, expectedTasks, processVariableKeys);

        StopWatch stopWatch = new StopWatch();

        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            entityManager.clear();
            stopWatch.start();
            findTasksOriginal(processVariableKeys, pageable);
            stopWatch.stop();
        }

        List<Long> list = Arrays.stream(stopWatch.getTaskInfo()).map(StopWatch.TaskInfo::getTimeMillis).toList();

        double averageTime = list.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);

        double standardDeviation = Math.sqrt(
            list
                .stream()
                .mapToDouble(Long::doubleValue)
                .map(value -> Math.pow(value - averageTime, 2))
                .average()
                .orElse(0.0)
        );

        originalApproachResults.add(
            new Integer[] {
                numOfProcessVarFilters,
                numOfProcessVarsToFetch,
                (int) averageTime,
                (int) standardDeviation,
            }
        );
    }

    private static void doAssert(
        PagedModel<EntityModel<TaskDto>> response,
        Set<ProcessVariableValueFilter> processVariableValueFilters,
        List<TaskEntity> tasks,
        List<String> expectedTasks,
        Set<ProcessVariableKey> processVariableKeys
    ) {
        List<TaskDto> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        if (processVariableValueFilters.isEmpty()) {
            assertThat(retrievedTasks).hasSameSizeAs(tasks);
        } else {
            assertThat(retrievedTasks).hasSameSizeAs(expectedTasks);
            assertThat(expectedTasks)
                .containsExactlyInAnyOrderElementsOf(retrievedTasks.stream().map(TaskDto::getId).toList());
        }
        if (processVariableKeys.isEmpty()) {
            assertThat(retrievedTasks).allSatisfy(task -> assertThat(task.getProcessVariables()).isNullOrEmpty());
        } else {
            assertThat(retrievedTasks)
                .allSatisfy(task ->
                    assertThat(task.getProcessVariables())
                        .allSatisfy(pv -> {
                            processVariableValueFilters
                                .stream()
                                .filter(filter -> filter.name().equals(pv.name()))
                                .findAny()
                                .ifPresent(filter -> assertThat(pv.value()).isEqualTo(filter.value()));
                            assertThat(processVariableKeys)
                                .extracting(ProcessVariableKey::variableName)
                                .contains(pv.name());
                        })
                );
        }
    }

    private PagedModel<EntityModel<TaskDto>> findTasksApproach1(
        Set<ProcessVariableValueFilter> processVariableValueFilters,
        Set<ProcessVariableKey> processVariableKeys,
        Pageable pageable
    ) {
        return taskControllerHelper.searchTaskApproach1(
            new TaskSearchRequest(
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
                processVariableValueFilters,
                processVariableKeys
            ),
            pageable
        );
    }

    private PagedModel<EntityModel<TaskDto>> findTasksApproach2(
        Set<ProcessVariableValueFilter> processVariableValueFilters,
        Set<ProcessVariableKey> processVariableKeys,
        Pageable pageable
    ) {
        return taskControllerHelper.searchTaskApproach2(
            new TaskSearchRequest(
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
                processVariableValueFilters,
                processVariableKeys
            ),
            pageable
        );
    }

    private PagedModel<EntityModel<TaskDto>> findTasksOriginal(
        Set<ProcessVariableKey> processVariableKeys,
        Pageable pageable
    ) {
        return taskControllerHelper.findAllWithProcessVariables(
            null,
            new VariableSearch(null, null, null),
            pageable,
            Collections.emptyList(),
            processVariableKeys.stream().map(pv -> pv.processDefinitionKey() + "/" + pv.variableName()).toList()
        );
    }
}
