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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.QueryRestTestApplication;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.specification.TaskSpecification;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
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
class TaskAssignedCountIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private QueryTestUtils queryTestUtils;

    @BeforeEach
    void setUp() {
        queryTestUtils.cleanUp();
    }

    @Test
    void shouldCountOnlyAssignedTasksGroupedByAssignee_whenSomeUsersHaveNoAssignedTask() {
        queryTestUtils.buildTask().withAssignee("alice").buildAndSave();
        queryTestUtils.buildTask().withAssignee("alice").buildAndSave();
        queryTestUtils.buildTask().withAssignee("bob").buildAndSave();
        // The same users' non-ASSIGNED tasks must not be counted.
        queryTestUtils.buildTask().withAssignee("bob").withStatus(Task.TaskStatus.SUSPENDED).buildAndSave();
        queryTestUtils.buildTask().withAssignee("bob").withStatus(Task.TaskStatus.COMPLETED).buildAndSave();
        // An unassigned, created task belongs to no assignee.
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).buildAndSave();

        Map<String, Long> counts = countAssigned(Set.of("alice", "bob", "carol"));

        // carol has no assigned task, so there is no row for her (zero needs no special case).
        assertThat(counts).containsOnly(entry("alice", 2L), entry("bob", 1L));
    }

    @Test
    void shouldMatchTheRestrictedRestCountPerUser_whenCountingAssignedTasks() {
        queryTestUtils.buildTask().withAssignee("alice").buildAndSave();
        queryTestUtils.buildTask().withAssignee("alice").buildAndSave();
        queryTestUtils.buildTask().withAssignee("bob").buildAndSave();
        queryTestUtils.buildTask().withAssignee("bob").withStatus(Task.TaskStatus.SUSPENDED).buildAndSave();
        // Owned by alice but assigned to bob: it is bob's assigned task, never alice's.
        queryTestUtils.buildTask().withAssignee("bob").withOwner("alice").buildAndSave();

        Map<String, Long> grouped = countAssigned(Set.of("alice", "bob"));

        assertThat(grouped.getOrDefault("alice", 0L)).isEqualTo(restrictedAssignedCount("alice"));
        assertThat(grouped.getOrDefault("bob", 0L)).isEqualTo(restrictedAssignedCount("bob"));
    }

    private Map<String, Long> countAssigned(Set<String> users) {
        return taskRepository
            .countGroupedByAssignee(users, Task.TaskStatus.ASSIGNED)
            .stream()
            .collect(
                Collectors.toMap(TaskRepository.AssigneeCount::getAssignee, TaskRepository.AssigneeCount::getTaskCount)
            );
    }

    private long restrictedAssignedCount(String userId) {
        TaskSearchRequest request = new TaskSearchRequestBuilder()
            .withStatus(Task.TaskStatus.ASSIGNED)
            .withAssignees(userId)
            .build();
        return taskRepository.count(TaskSpecification.restricted(request, userId, List.of()));
    }
}
