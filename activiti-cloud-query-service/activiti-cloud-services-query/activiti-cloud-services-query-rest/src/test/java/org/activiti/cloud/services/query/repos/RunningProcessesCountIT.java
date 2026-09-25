/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.repos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.activiti.QueryRestTestApplication;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.app.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.specification.ProcessInstanceSpecification;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    classes = { QueryRestTestApplication.class },
    properties = {
        "spring.main.banner-mode=off", "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
@Transactional
class RunningProcessesCountIT {

    private static final ProcessInstance.ProcessInstanceStatus RUNNING = ProcessInstance.ProcessInstanceStatus.RUNNING;
    private static final ProcessInstance.ProcessInstanceStatus COMPLETED =
        ProcessInstance.ProcessInstanceStatus.COMPLETED;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private QueryTestUtils queryTestUtils;

    @BeforeEach
    void setUp() {
        queryTestUtils.cleanUp();
    }

    @Test
    void shouldFindRunningProcessesByInitiator_excludingNonRunningAndOutsideRequestedSet() {
        queryTestUtils.buildProcessInstance().withInitiator("alice").withStatus(RUNNING).buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator("alice").withStatus(COMPLETED).buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator("bob").withStatus(RUNNING).buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator("dave").withStatus(RUNNING).buildAndSave();

        Map<String, Set<String>> byUser = groupByInitiator(
            processInstanceRepository.findRunningByInitiatorIn(Set.of("alice", "bob"), RUNNING)
        );

        assertThat(byUser.get("alice")).hasSize(1);
        assertThat(byUser.get("bob")).hasSize(1);
        assertThat(byUser).doesNotContainKey("dave");
    }

    @Test
    void shouldFindRunningProcessesByAssignee_excludingNonRunning() {
        queryTestUtils
            .buildProcessInstance()
            .withStatus(RUNNING)
            .withTasks(queryTestUtils.buildTask().withAssignee("alice"))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withStatus(COMPLETED)
            .withTasks(queryTestUtils.buildTask().withAssignee("alice"))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withStatus(RUNNING)
            .withTasks(queryTestUtils.buildTask().withAssignee("bob"))
            .buildAndSave();

        Map<String, Set<String>> byUser = groupByAssignee(
            taskRepository.findRunningProcessesByAssigneeIn(Set.of("alice", "bob"), RUNNING)
        );

        assertThat(byUser.get("alice")).hasSize(1);
        assertThat(byUser.get("bob")).hasSize(1);
    }

    @Test
    void shouldFindRunningProcessesByCandidateUser_excludingNonRunning() {
        queryTestUtils
            .buildProcessInstance()
            .withStatus(RUNNING)
            .withTasks(queryTestUtils.buildTask().withTaskCandidateUsers("alice"))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withStatus(COMPLETED)
            .withTasks(queryTestUtils.buildTask().withTaskCandidateUsers("alice"))
            .buildAndSave();

        Map<String, Set<String>> byUser = groupByCandidateUser(
            taskCandidateUserRepository.findRunningProcessesByCandidateUserIn(Set.of("alice"), RUNNING)
        );

        assertThat(byUser.get("alice")).hasSize(1);
    }

    @Test
    void shouldCountAProcessOnce_whenVisibleThroughTwoDoorsAtOnce() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator("alice")
            .withStatus(RUNNING)
            .withTasks(queryTestUtils.buildTask().withAssignee("alice"))
            .buildAndSave();

        Map<String, Long> counts = runningProcessCountsFor(Set.of("alice"));

        assertThat(counts).containsOnly(entry("alice", 1L));
    }

    @Test
    void shouldMatchTheRestrictedRestCountPerUser_whenCountingRunningProcesses() {
        queryTestUtils.buildProcessInstance().withInitiator("alice").withStatus(RUNNING).buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withStatus(RUNNING)
            .withTasks(queryTestUtils.buildTask().withAssignee("bob"))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withStatus(RUNNING)
            .withTasks(queryTestUtils.buildTask().withTaskCandidateUsers("carol"))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator("alice").withStatus(COMPLETED).buildAndSave();

        Map<String, Long> counts = runningProcessCountsFor(Set.of("alice", "bob", "carol"));

        assertThat(counts.getOrDefault("alice", 0L)).isEqualTo(restrictedRunningProcessCount("alice"));
        assertThat(counts.getOrDefault("bob", 0L)).isEqualTo(restrictedRunningProcessCount("bob"));
        assertThat(counts.getOrDefault("carol", 0L)).isEqualTo(restrictedRunningProcessCount("carol"));
    }

    /** Mirrors RunningProcessesCounter's merge logic; this module doesn't depend on that class. */
    private Map<String, Long> runningProcessCountsFor(Set<String> userIds) {
        Map<String, Set<String>> visible = new HashMap<>();
        processInstanceRepository
            .findRunningByInitiatorIn(userIds, RUNNING)
            .forEach(row ->
                visible.computeIfAbsent(row.getUserId(), key -> new HashSet<>()).add(row.getProcessInstanceId())
            );
        taskRepository
            .findRunningProcessesByAssigneeIn(userIds, RUNNING)
            .forEach(row ->
                visible.computeIfAbsent(row.getUserId(), key -> new HashSet<>()).add(row.getProcessInstanceId())
            );
        taskCandidateUserRepository
            .findRunningProcessesByCandidateUserIn(userIds, RUNNING)
            .forEach(row ->
                visible.computeIfAbsent(row.getUserId(), key -> new HashSet<>()).add(row.getProcessInstanceId())
            );
        return userIds
            .stream()
            .collect(Collectors.toMap(Function.identity(), id -> (long) visible.getOrDefault(id, Set.of()).size()));
    }

    private long restrictedRunningProcessCount(String userId) {
        ProcessInstanceSearchRequest request = new ProcessInstanceSearchRequest();
        request.setStatus(Set.of(RUNNING));
        return processInstanceRepository.count(ProcessInstanceSpecification.restricted(request, userId));
    }

    private static Map<String, Set<String>> groupByInitiator(
        Iterable<ProcessInstanceRepository.InitiatorProcess> rows
    ) {
        Map<String, Set<String>> byUser = new HashMap<>();
        rows.forEach(row ->
            byUser.computeIfAbsent(row.getUserId(), key -> new HashSet<>()).add(row.getProcessInstanceId())
        );
        return byUser;
    }

    private static Map<String, Set<String>> groupByAssignee(Iterable<TaskRepository.AssigneeProcess> rows) {
        Map<String, Set<String>> byUser = new HashMap<>();
        rows.forEach(row ->
            byUser.computeIfAbsent(row.getUserId(), key -> new HashSet<>()).add(row.getProcessInstanceId())
        );
        return byUser;
    }

    private static Map<String, Set<String>> groupByCandidateUser(
        Iterable<TaskCandidateUserRepository.CandidateProcess> rows
    ) {
        Map<String, Set<String>> byUser = new HashMap<>();
        rows.forEach(row ->
            byUser.computeIfAbsent(row.getUserId(), key -> new HashSet<>()).add(row.getProcessInstanceId())
        );
        return byUser;
    }
}
