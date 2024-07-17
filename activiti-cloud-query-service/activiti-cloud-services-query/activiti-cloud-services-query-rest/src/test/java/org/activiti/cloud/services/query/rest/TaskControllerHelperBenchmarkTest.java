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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessVariablesPivotRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableFilterType;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.ProcessVariableValueFilter;
import org.activiti.cloud.services.query.model.ProcessVariablesPivotEntity;
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
    private ProcessVariablesPivotRepository processVariablesPivotRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final List<Integer[]> originalApproachResults = new ArrayList<>();
    private static final List<Integer[]> approach1Results = new ArrayList<>();
    private static final List<Integer[]> approach2Results = new ArrayList<>();

    private static final int NUMBER_OF_ITERATIONS = 500;
    private static final int NUMBER_OF_PROCESS_INSTANCES = 10000;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        taskVariableRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
    }

    @AfterAll
    public static void printResults() {
        printResults("ORIGINAL APPROACH", originalApproachResults);
        printResults("APPROACH 1", approach1Results);
        printResults("APPROACH 2", approach2Results);
        printCSV();
    }

    private static void printResults(String title, List<Integer[]> results) {
        System.out.print("\n\n");
        String[] header = { "#filters", "#fetchVars", "avg time (ms)", "std dev (ms)" };
        String format = "%-12s%-12s%-15s%-15s%n";
        String separator = "--------------------------------------------------------";
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

    private static void printCSV() {
        System.out.println("ORIGINAL APPROACH");
        System.out.println("#filters,#fetchVars,avg time (ms),std dev (ms)");
        originalApproachResults.forEach(r -> System.out.println(r[0] + "," + r[1] + "," + r[2] + "," + r[3]));
        System.out.println("APPROACH 1");
        System.out.println("#filters,#fetchVars,avg time (ms),std dev (ms)");
        approach1Results.forEach(r -> System.out.println(r[0] + "," + r[1] + "," + r[2] + "," + r[3]));
        System.out.println("APPROACH 2");
        System.out.println("#filters,#fetchVars,avg time (ms),std dev (ms)");
        approach2Results.forEach(r -> System.out.println(r[0] + "," + r[1] + "," + r[2] + "," + r[3]));
    }

    private static Stream<Arguments> getTestParameters() {
        return Stream.of(
            Arguments.of(0, 0),
            Arguments.of(0, 1),
            Arguments.of(0, 2),
            Arguments.of(0, 5),
            Arguments.of(0, 10),
            Arguments.of(0, 15),
            Arguments.of(1, 0),
            Arguments.of(2, 0),
            Arguments.of(5, 0),
            Arguments.of(10, 0),
            Arguments.of(15, 0),
            Arguments.of(1, 1),
            Arguments.of(2, 1),
            Arguments.of(5, 1),
            Arguments.of(10, 1),
            Arguments.of(15, 1),
            Arguments.of(1, 2),
            Arguments.of(1, 5),
            Arguments.of(1, 10),
            Arguments.of(1, 15),
            Arguments.of(2, 2),
            Arguments.of(5, 5),
            Arguments.of(10, 10),
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

        Map<String, String> processVariableNamesAndValues = IntStream
            .range(0, 16)
            .mapToObj(i -> Map.entry("var" + i, "value" + i))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<ProcessInstanceEntity> processInstances = IntStream
            .range(0, NUMBER_OF_PROCESS_INSTANCES)
            .parallel()
            .mapToObj(i -> {
                ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
                processInstanceEntity.setId(UUID.randomUUID().toString());
                processInstanceEntity.setName("name" + i);
                processInstanceEntity.setInitiator("initiator");
                processInstanceEntity.setProcessDefinitionName("test");
                processInstanceEntity.setProcessDefinitionKey("processDefinitionKey");
                processInstanceEntity.setServiceName("test");

                processInstanceRepository.save(processInstanceEntity);

                Set<ProcessVariableEntity> processVariables = processVariableNamesAndValues
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        ProcessVariableEntity processVar = new ProcessVariableEntity();
                        processVar.setName(entry.getKey());
                        processVar.setValue(entry.getValue());
                        processVar.setProcessDefinitionKey(processDefinitionKey);
                        processVar.setProcessInstanceId(processInstanceEntity.getId());
                        return processVar;
                    })
                    .collect(Collectors.toSet());
                variableRepository.saveAll(processVariables);
                processInstanceEntity.setVariables(processVariables);

                TaskEntity taskEntity = new TaskEntity();
                String taskId = UUID.randomUUID().toString();
                taskEntity.setId(taskId);
                taskEntity.setCreatedDate(new Date());
                taskEntity.setProcessVariables(processInstanceEntity.getVariables());
                taskEntity.setProcessInstanceId(processInstanceEntity.getId());
                taskRepository.save(taskEntity);
                processInstanceEntity.setTasks(Set.of(taskEntity));

                ProcessVariablesPivotEntity pivot = processVariablesPivotRepository
                    .findById(processInstanceEntity.getId())
                    .orElseGet(() -> {
                        ProcessVariablesPivotEntity p = new ProcessVariablesPivotEntity();
                        p.setProcessInstanceId(processInstanceEntity.getId());
                        p.setValues(new HashMap<>());
                        return p;
                    });

                Map<String, Object> processInstanceIdVariables = processInstanceEntity
                    .getVariables()
                    .stream()
                    .map(pv -> Map.entry(pv.getProcessDefinitionKey() + "/" + pv.getName(), pv.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                pivot.setValues(processInstanceIdVariables);
                processVariablesPivotRepository.save(pivot);

                return processInstanceEntity;
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
                    ProcessVariablesPivotEntity pivot = processVariablesPivotRepository
                        .findById(processInstance.getId())
                        .get();
                    pivot.getValues().put(processDefinitionKey + "/" + processVariable.getName(), valueToSearch);
                    processVariablesPivotRepository.save(pivot);
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

        Pageable pageable = PageRequest.of(0, 1000);

        testOriginalApproach(
            numOfProcessVarFilters,
            numOfProcessVarsToFetch,
            processVariableKeys,
            expectedTasks,
            pageable
        );
        testApproach1(
            numOfProcessVarFilters,
            numOfProcessVarsToFetch,
            processVariableValueFilters,
            processVariableKeys,
            expectedTasks,
            pageable
        );
        testApproach2(
            numOfProcessVarFilters,
            numOfProcessVarsToFetch,
            processVariableValueFilters,
            processVariableKeys,
            expectedTasks,
            pageable
        );
    }

    private void testApproach1(
        int numOfProcessVarFilters,
        int numOfProcessVarsToFetch,
        Set<ProcessVariableValueFilter> processVariableValueFilters,
        Set<ProcessVariableKey> processVariableKeys,
        List<String> expectedTasks,
        Pageable pageable
    ) {
        PagedModel<EntityModel<TaskDto>> response = findTasksApproach1(
            processVariableValueFilters,
            processVariableKeys,
            pageable
        );
        doAssert(response, processVariableValueFilters, expectedTasks, processVariableKeys, pageable);

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
        List<String> expectedTasks,
        Pageable pageable
    ) {
        PagedModel<EntityModel<TaskDto>> response = findTasksApproach2(
            processVariableValueFilters,
            processVariableKeys,
            pageable
        );
        doAssert(response, processVariableValueFilters, expectedTasks, processVariableKeys, pageable);

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
        List<String> expectedTasks,
        Pageable pageable
    ) {
        if (numOfProcessVarFilters != 0) {
            return;
        }

        PagedModel<EntityModel<TaskDto>> response = findTasksOriginal(processVariableKeys, pageable);
        doAssert(response, Collections.emptySet(), expectedTasks, processVariableKeys, pageable);

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
        List<String> expectedTasks,
        Set<ProcessVariableKey> processVariableKeys,
        Pageable pageable
    ) {
        List<TaskDto> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        if (processVariableValueFilters.isEmpty()) {
            assertThat(retrievedTasks).hasSize(Math.min(NUMBER_OF_PROCESS_INSTANCES, pageable.getPageSize()));
        } else {
            assertThat(retrievedTasks).hasSize(Math.min(expectedTasks.size(), pageable.getPageSize()));
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
