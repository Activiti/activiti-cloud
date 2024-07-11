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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.activiti.cloud.services.query.model.ProcessVariablesPivotEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.dto.TaskDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
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

    @Autowired
    private ProcessVariablesPivotRepository processVariablesPivotRepository;

    private static final Map<String, Object> results = new LinkedHashMap<>();

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
        results.forEach((key, value) -> System.out.println(key + " | " + value));
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
            .range(0, 1000)
            .mapToObj(i -> {
                ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
                processInstanceEntity.setId(UUID.randomUUID().toString());
                processInstanceEntity.setName("name" + i);
                processInstanceEntity.setInitiator("initiator");
                processInstanceEntity.setProcessDefinitionName("test");
                processInstanceEntity.setProcessDefinitionKey("processDefinitionKey");
                processInstanceEntity.setServiceName("test");
                processInstanceRepository.save(processInstanceEntity);
                return processInstanceEntity;
            })
            .collect(Collectors.toList());

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
                    variableRepository.save(processVar);
                    return processVar;
                })
                .collect(Collectors.toSet());
            processInstance.setVariables(processVariables);
            processInstanceRepository.save(processInstance);
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
                processInstanceRepository.save(processInstance);

                ProcessVariablesPivotEntity pivot = processVariablesPivotRepository
                    .findById(processInstance.getId())
                    .orElseGet(() -> {
                        ProcessVariablesPivotEntity p = new ProcessVariablesPivotEntity();
                        p.setProcessInstanceId(processInstance.getId());
                        p.setValues(new HashMap<>());
                        return p;
                    });

                Map<String, Object> processInstanceIdVariables = processInstance
                    .getVariables()
                    .stream()
                    .map(pv -> Map.entry(pv.getProcessDefinitionKey() + "/" + pv.getName(), pv.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                pivot.setValues(processInstanceIdVariables);
                processVariablesPivotRepository.save(pivot);
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

        //first invocation to assert that result are correct
        PagedModel<EntityModel<TaskDto>> response = findTasks(processVariableValueFilters, processVariableKeys);
        doAssert(response, processVariableValueFilters, tasks, expectedTasks, processVariableKeys);

        StopWatch stopWatch = new StopWatch();

        for (int i = 0; i < 100; i++) {
            stopWatch.start();
            findTasks(processVariableValueFilters, processVariableKeys);
            stopWatch.stop();
        }

        double averageTime = (double) stopWatch.getTotalTimeMillis() / stopWatch.getTaskCount();

        results.put(
            String.format(
                "number of process variable filters: %d | number of process variables to return: %d",
                numOfProcessVarFilters,
                numOfProcessVarsToFetch
            ),
            "avg response time(ms): " + averageTime
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
                        .allSatisfy((key, value) -> {
                            processVariableValueFilters
                                .stream()
                                .filter(filter -> filter.name().equals(key))
                                .findAny()
                                .ifPresent(filter -> assertThat(value).isEqualTo(filter.value()));
                            assertThat(processVariableKeys).extracting(ProcessVariableKey::variableName).contains(key);
                        })
                );
        }
    }

    private PagedModel<EntityModel<TaskDto>> findTasks(
        Set<ProcessVariableValueFilter> processVariableValueFilters,
        Set<ProcessVariableKey> processVariableKeys
    ) {
        return taskControllerHelper.findAllWithProcessVariables(
            null,
            new VariableSearch(null, null, null),
            PageRequest.of(0, 10000),
            Collections.emptyList(),
            processVariableValueFilters,
            processVariableKeys
        );
    }
}
