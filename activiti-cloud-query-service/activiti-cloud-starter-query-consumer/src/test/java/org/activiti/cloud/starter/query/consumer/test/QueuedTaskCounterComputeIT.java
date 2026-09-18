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
package org.activiti.cloud.starter.query.consumer.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.ConsumerSubscriberRegistry;
import org.activiti.cloud.services.query.app.QueuedTaskCounter;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end for the real {@link QueuedTaskCounter} bean: seeds tasks and candidacies in the database
 * and a live {@link ConsumerSubscriberRegistry}, then drives {@code compute} against a real repository
 * (not mocks). Complements {@code QueuedTaskCounterTest} (orchestration, mocked repo) and the query-rest
 * {@code TaskQueuedCountIT} (query/spec semantics, called directly) by exercising the whole path together.
 */
@SpringBootTest(
    classes = QueryConsumerTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "activiti.cloud.services.oauth2.iam-name=test",
        "activiti.cloud.query.pushed-counts.enabled=true",
        "activiti.features.query.pushed-counts.enabled=true",
    }
)
@EnableTestBinder
// Own context (marker profile) so seeding the in-memory registry can't collide with other starter ITs.
@ActiveProfiles("queued-counter-compute-it")
@Transactional
class QueuedTaskCounterComputeIT {

    private static final String SOURCE = "rest-1";

    @Autowired
    private QueuedTaskCounter queuedTaskCounter;

    @Autowired
    private ConsumerSubscriberRegistry registry;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @BeforeEach
    void cleanDatabase() {
        taskCandidateUserRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @AfterEach
    void cleanRegistry() {
        // Registry is in-memory (not rolled back with the transaction), so drop what this test added.
        Set.of("alice", "bob", "carol", "dave").forEach(user -> registry.unregister(user, SOURCE, Instant.now()));
    }

    @Test
    void computesQueuedCountsAcrossGroupSetBuckets_withoutDoubleCounting() {
        registry.register("alice", Set.of("g1"), SOURCE, Instant.now());
        registry.register("bob", Set.of("g1"), SOURCE, Instant.now());
        registry.register("carol", Set.of("g2"), SOURCE, Instant.now());

        saveTask("t1", Task.TaskStatus.CREATED, null, Set.of(), Set.of("g1")); // group-visible via g1
        saveTask("t2", Task.TaskStatus.CREATED, null, Set.of("alice"), Set.of("g1")); // group + personal: shared only
        saveTask("t3", Task.TaskStatus.CREATED, null, Set.of(), Set.of()); // open to everyone (no candidates)
        saveTask("t4", Task.TaskStatus.CREATED, null, Set.of("alice"), Set.of()); // alice's personal remainder
        saveTask("t5", Task.TaskStatus.CREATED, null, Set.of(), Set.of("g2")); // group-visible via g2
        saveTask("t6", Task.TaskStatus.ASSIGNED, "dave", Set.of(), Set.of("g1")); // claimed: not queued

        Map<String, Long> counts = queuedTaskCounter.compute(Set.of("alice", "bob", "carol"));

        // alice = shared{t1,t2,t3} + remainder{t4}; bob = shared only; carol = shared{t3,t5}. t2 is not double-counted.
        assertThat(counts).containsOnly(entry("alice", 4L), entry("bob", 3L), entry("carol", 2L));
    }

    @Test
    void materializesZero_forAWatchedUserWithNothingClaimable() {
        registry.register("dave", Set.of("g9"), SOURCE, Instant.now());
        saveTask("t1", Task.TaskStatus.CREATED, null, Set.of(), Set.of("g1")); // neither g9-visible nor open

        Map<String, Long> counts = queuedTaskCounter.compute(Set.of("dave"));

        assertThat(counts).containsOnly(entry("dave", 0L));
    }

    private void saveTask(
        String id,
        Task.TaskStatus status,
        String assignee,
        Set<String> candidateUsers,
        Set<String> candidateGroups
    ) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setName(id);
        if (assignee != null) {
            task.setAssignee(assignee);
        }
        task.setStatus(status);
        task.setCreatedDate(new Date());
        taskRepository.save(task);
        candidateUsers.forEach(user -> taskCandidateUserRepository.save(new TaskCandidateUserEntity(id, user)));
        candidateGroups.forEach(group -> taskCandidateGroupRepository.save(new TaskCandidateGroupEntity(id, group)));
    }
}
