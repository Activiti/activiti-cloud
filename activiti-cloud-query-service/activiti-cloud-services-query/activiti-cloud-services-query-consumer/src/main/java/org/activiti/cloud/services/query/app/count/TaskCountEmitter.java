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
package org.activiti.cloud.services.query.app.count;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.TaskCandidateGroup;
import org.activiti.api.task.model.TaskCandidateUser;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads an event batch, works out which counts it could have changed, and publishes the new ones.
 * <p>
 * The entry point for the whole feature: a batch of events arrives, commits, and this turns it into
 * "audience X now has N queued tasks" messages. It reads the task ids straight off the events rather
 * than having the event handlers collect them, which keeps the handlers untouched.
 * <p>
 * Must be called <em>after</em> the batch commits, so a rolled-back batch publishes nothing and the
 * counts it publishes are ones a client could also have read over REST.
 */
public class TaskCountEmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCountEmitter.class);

    private final TaskCountRecomputer recomputer;
    private final TaskCountChangePublisher publisher;
    private final Clock clock;

    public TaskCountEmitter(TaskCountRecomputer recomputer, TaskCountChangePublisher publisher, Clock clock) {
        this.recomputer = recomputer;
        this.publisher = publisher;
        this.clock = clock;
    }

    public void emitFor(List<CloudRuntimeEvent<?, ?>> events) {
        Set<String> taskIds = new HashSet<>();
        Set<String> groupsNamedInBatch = new HashSet<>();
        Set<String> usersNamedInBatch = new HashSet<>();
        collect(events, taskIds, groupsNamedInBatch, usersNamedInBatch);

        if (taskIds.isEmpty() && groupsNamedInBatch.isEmpty() && usersNamedInBatch.isEmpty()) {
            // Nothing task-shaped in the batch - process and variable events cannot move a task count.
            return;
        }

        // Deliberately not transactional here: the recomputer opens its own transaction, so a batch that
        // turns out to have no audience costs no transaction at all.
        List<TaskCountChange> changes;
        try {
            changes = recomputer.recompute(taskIds, groupsNamedInBatch, usersNamedInBatch);
        } catch (RuntimeException cause) {
            // The batch has already committed. Failing to push a count means clients fall back to the
            // count they poll for, which is a degradation, not a corruption - so log and carry on.
            LOGGER.error("Could not recompute task counts for {} task(s); counts will be stale", taskIds.size(), cause);
            return;
        }
        if (changes.isEmpty()) {
            return;
        }

        long asOf = clock.millis();
        publisher.publish(
            changes
                .stream()
                .map(change -> TaskCountChangedEvent.of(change, asOf))
                .toList()
        );
    }

    /**
     * Task ids, and the groups and users the batch named directly.
     * <p>
     * Candidate events are the reason identities are collected here at all, and it is a
     * <em>removal</em> that makes it necessary: a {@code TASK_CANDIDATE_GROUP_REMOVED} or
     * {@code TASK_CANDIDATE_USER_REMOVED} event names the group or user whose count just changed, and once
     * the batch has committed that row is gone, so reading it back is impossible. Everything else can be
     * recovered from the task ids.
     */
    private static void collect(
        List<CloudRuntimeEvent<?, ?>> events,
        Set<String> taskIds,
        Set<String> groupsNamedInBatch,
        Set<String> usersNamedInBatch
    ) {
        for (CloudRuntimeEvent<?, ?> event : events) {
            switch (event.getEntity()) {
                case Task task -> addIfPresent(taskIds, task.getId());
                case TaskCandidateGroup candidateGroup -> {
                    addIfPresent(taskIds, candidateGroup.getTaskId());
                    addIfPresent(groupsNamedInBatch, candidateGroup.getGroupId());
                }
                case TaskCandidateUser candidateUser -> {
                    addIfPresent(taskIds, candidateUser.getTaskId());
                    addIfPresent(usersNamedInBatch, candidateUser.getUserId());
                }
                case null, default -> {
                    // Not a task event: cannot change a task count.
                }
            }
        }
    }

    private static void addIfPresent(Set<String> target, String value) {
        if (value != null) {
            target.add(value);
        }
    }
}
