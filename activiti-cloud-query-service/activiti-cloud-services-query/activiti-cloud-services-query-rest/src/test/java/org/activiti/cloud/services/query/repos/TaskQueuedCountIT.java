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
class TaskQueuedCountIT {

    // Matches no assignee, owner or candidate user, so the restricted specification yields the group-only
    // (shared) visibility: assignee is null and (a candidate group is in the set or the task has none).
    private static final String GROUP_VISIBILITY_PROBE = "__queued_shared_visibility_probe__";

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
    void shouldExcludeGroupVisibleTasksFromTheRemainder_whenATaskIsVisibleViaBothAGroupAndTheUser() {
        // Personal-only: alice is named on it and no group can see it.
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateUsers("alice").buildAndSave();
        // Both personal AND group-visible via g1: it must NOT appear in the remainder, or summing it with the
        // shared group count would count it twice.
        queryTestUtils
            .buildTask()
            .withStatus(Task.TaskStatus.CREATED)
            .withTaskCandidateUsers("alice")
            .withTaskCandidateGroups("g1")
            .buildAndSave();

        Map<String, Long> remainder = remainder(Set.of("alice"), Set.of("g1"));

        // Only the personal-only task; if the exclusion were dropped this would be 2.
        assertThat(remainder).containsOnly(entry("alice", 1L));
    }

    @Test
    void shouldEqualTheRestrictedCount_whenSummingSharedAndPersonalRemainder() {
        Set<String> aliceGroups = Set.of("g1", "g1b");
        // Shared: visible to anyone in the group set.
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).buildAndSave(); // open to all (no candidates)
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateGroups("g1").buildAndSave();
        // Two candidate-group rows in the set on ONE task: it must still be counted once (distinct on task).
        queryTestUtils
            .buildTask()
            .withStatus(Task.TaskStatus.CREATED)
            .withTaskCandidateGroups("g1", "g1b")
            .buildAndSave();
        // Both alice-personal and group-visible: belongs to the shared count, not the remainder.
        queryTestUtils
            .buildTask()
            .withStatus(Task.TaskStatus.CREATED)
            .withTaskCandidateUsers("alice")
            .withTaskCandidateGroups("g1")
            .buildAndSave();
        // Personal-only extra for alice.
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateUsers("alice").buildAndSave();
        // Not visible to alice: only a group she is not in.
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateGroups("g2").buildAndSave();
        // Not queued: already assigned, even though a group could otherwise see it.
        queryTestUtils.buildTask().withAssignee("someone").withTaskCandidateGroups("g1").buildAndSave();

        long shared = sharedGroupVisibleCount(aliceGroups);
        long remainder = remainder(Set.of("alice"), aliceGroups).getOrDefault("alice", 0L);

        assertThat(shared).isEqualTo(4L); // open + g1 + (g1,g1b once) + (alice&g1)
        assertThat(remainder).isEqualTo(1L); // personal-only
        assertThat(shared + remainder).isEqualTo(restrictedQueuedCount("alice", aliceGroups)).isEqualTo(5L);
    }

    @Test
    void shouldCountOpenTasksAndPersonalCandidacyOnly_whenTheUserHasNoGroups() {
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).buildAndSave(); // open to all
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateUsers("bob").buildAndSave();
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateGroups("g1").buildAndSave();

        long shared = sharedGroupVisibleCount(Set.of());
        long remainder = remainder(Set.of("bob"), Set.of()).getOrDefault("bob", 0L);

        assertThat(shared).isEqualTo(1L); // only the open task
        assertThat(remainder).isEqualTo(1L); // bob's personal candidacy
        assertThat(shared + remainder).isEqualTo(restrictedQueuedCount("bob", Set.of())).isEqualTo(2L);
    }

    @Test
    void shouldReturnOneRowPerMemberWithExtras_whenSomeBucketMembersHaveNoRemainder() {
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateUsers("alice").buildAndSave();
        // bob shares the bucket but has no personal-only task.
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateGroups("g1").buildAndSave();

        Map<String, Long> remainder = remainder(Set.of("alice", "bob"), Set.of("g1"));

        assertThat(remainder).containsOnly(entry("alice", 1L));
    }

    @Test
    void shouldNotCountATask_whenItIsNotCreatedOrIsAssignedToSomeoneElse() {
        // Suspended, completed and cancelled tasks are not claimable, whatever the candidacy.
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.SUSPENDED).withTaskCandidateGroups("g1").buildAndSave();
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.COMPLETED).withTaskCandidateUsers("alice").buildAndSave();
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CANCELLED).buildAndSave();
        // Already claimed by someone else, even though alice would otherwise be eligible for it.
        queryTestUtils
            .buildTask()
            .withAssignee("bob")
            .withTaskCandidateUsers("alice")
            .withTaskCandidateGroups("g1")
            .buildAndSave();

        long shared = sharedGroupVisibleCount(Set.of("g1"));
        long remainder = remainder(Set.of("alice"), Set.of("g1")).getOrDefault("alice", 0L);

        assertThat(shared).isZero();
        assertThat(remainder).isZero();
        assertThat(shared + remainder).isEqualTo(restrictedQueuedCount("alice", Set.of("g1"))).isZero();
    }

    @Test
    void shouldNotCountAnOwnedTask_whenTheUserOwnsItButIsNotACandidate() {
        // Unassigned CREATED task alice owns but is not a candidate for (a foreign candidate keeps it from
        // being open-to-all). Owning a task does not make it claimable, so it is deliberately not queued.
        queryTestUtils
            .buildTask()
            .withStatus(Task.TaskStatus.CREATED)
            .withOwner("alice")
            .withTaskCandidateUsers("bob")
            .buildAndSave();

        long shared = sharedGroupVisibleCount(Set.of("g1"));
        long remainder = remainder(Set.of("alice"), Set.of("g1")).getOrDefault("alice", 0L);

        assertThat(shared + remainder).isZero();
        // The old restricted {status:CREATED} count did include owner-visible tasks: this is the single
        // intentional difference between the two approaches.
        assertThat(restrictedQueuedCount("alice", Set.of("g1"))).isEqualTo(1L);
    }

    @Test
    void shouldPlaceAPersonalTaskInTheRemainder_whenNoMemberGroupCanSeeIt() {
        // Foreign groups only (alice is in neither): she sees it personally, her groups do not, so it is a
        // remainder task — counted once despite two candidate-group rows.
        queryTestUtils
            .buildTask()
            .withStatus(Task.TaskStatus.CREATED)
            .withTaskCandidateUsers("alice")
            .withTaskCandidateGroups("g2", "g3")
            .buildAndSave();
        // A member group (g1) can see it, so it belongs to the shared count and must be kept out of the
        // remainder even though alice is also personally named and an extra foreign group is present.
        queryTestUtils
            .buildTask()
            .withStatus(Task.TaskStatus.CREATED)
            .withTaskCandidateUsers("alice")
            .withTaskCandidateGroups("g1", "g2")
            .buildAndSave();

        long shared = sharedGroupVisibleCount(Set.of("g1"));
        long remainder = remainder(Set.of("alice"), Set.of("g1")).getOrDefault("alice", 0L);

        assertThat(remainder).isEqualTo(1L); // only the foreign-groups task, counted once
        assertThat(shared).isEqualTo(1L); // only the member-group task
        assertThat(shared + remainder).isEqualTo(restrictedQueuedCount("alice", Set.of("g1"))).isEqualTo(2L);
    }

    private Map<String, Long> remainder(Set<String> users, Set<String> groups) {
        return taskRepository
            .countQueuedPersonalRemainderGroupedByUser(users, Task.TaskStatus.CREATED, groups)
            .stream()
            .collect(Collectors.toMap(TaskRepository.UserCount::getUserId, TaskRepository.UserCount::getTaskCount));
    }

    private long sharedGroupVisibleCount(Set<String> groups) {
        return taskRepository.count(TaskSpecification.restricted(queuedRequest(), GROUP_VISIBILITY_PROBE, groups));
    }

    private long restrictedQueuedCount(String userId, Set<String> groups) {
        return taskRepository.count(TaskSpecification.restricted(queuedRequest(), userId, groups));
    }

    private static TaskSearchRequest queuedRequest() {
        return new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.CREATED).build();
    }
}
