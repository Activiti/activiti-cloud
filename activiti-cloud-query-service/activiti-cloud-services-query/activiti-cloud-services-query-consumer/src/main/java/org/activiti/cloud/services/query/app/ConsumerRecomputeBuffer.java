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
package org.activiti.cloud.services.query.app;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Phase 1 of the recompute pipeline: an in-memory, event-time capture of what a committed event
 * batch touched, held until the next flush. Captures identities only - no queries - so a task or
 * process id lands here the moment an event names it, and a flush (time- or size-bound) later reads
 * back whatever else is needed to resolve who actually watches it.
 *
 * <p>Every access is synchronized: capture is called concurrently by the partitioned event-consumer
 * threads, and {@link #drainAndReset()} must never lose a capture that lands mid-drain.
 */
public final class ConsumerRecomputeBuffer {

    private final Set<String> taskIds = new HashSet<>();
    private final Set<String> touchedGroupIds = new HashSet<>();
    private final Set<String> namedUserIds = new HashSet<>();
    private final Set<String> processInstanceIds = new HashSet<>();
    private final Set<String> namedInitiatorIds = new HashSet<>();
    private Instant windowStartedAt;

    /** Records a touched task, and any users the event names directly (assignee, owner, completedBy). */
    public synchronized void captureTask(String taskId, Instant at, String... namedUsers) {
        if (taskId == null) {
            return;
        }
        markTouch(at);
        taskIds.add(taskId);
        for (String userId : namedUsers) {
            if (userId != null && !userId.isBlank()) {
                namedUserIds.add(userId);
            }
        }
    }

    /** Records a candidate-group add/remove on a task. */
    public synchronized void captureTaskCandidateGroup(String taskId, String groupId, Instant at) {
        if (taskId == null || groupId == null) {
            return;
        }
        markTouch(at);
        taskIds.add(taskId);
        touchedGroupIds.add(groupId);
    }

    /** Records a touched process instance, and its initiator if named on the event. */
    public synchronized void captureProcess(String processInstanceId, String initiator, Instant at) {
        if (processInstanceId == null) {
            return;
        }
        markTouch(at);
        processInstanceIds.add(processInstanceId);
        if (initiator != null && !initiator.isBlank()) {
            namedInitiatorIds.add(initiator);
        }
    }

    public synchronized boolean isEmpty() {
        return taskIds.isEmpty() && processInstanceIds.isEmpty();
    }

    /** Number of distinct touched tasks and processes - the size bound the scheduler checks. */
    public synchronized int size() {
        return taskIds.size() + processInstanceIds.size();
    }

    /** How long since the first capture of the current window - the time bound the scheduler checks. */
    public synchronized Duration age(Clock clock) {
        return windowStartedAt == null ? Duration.ZERO : Duration.between(windowStartedAt, clock.instant());
    }

    /** Atomically snapshots the window and clears the buffer for the next one. */
    public synchronized ConsumerRecomputeWindow drainAndReset() {
        ConsumerRecomputeWindow snapshot = new ConsumerRecomputeWindow(
            Set.copyOf(taskIds),
            Set.copyOf(touchedGroupIds),
            Set.copyOf(namedUserIds),
            Set.copyOf(processInstanceIds),
            Set.copyOf(namedInitiatorIds)
        );
        taskIds.clear();
        touchedGroupIds.clear();
        namedUserIds.clear();
        processInstanceIds.clear();
        namedInitiatorIds.clear();
        windowStartedAt = null;
        return snapshot;
    }

    private void markTouch(Instant at) {
        if (windowStartedAt == null) {
            windowStartedAt = at;
        }
    }
}
