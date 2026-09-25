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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Every method is synchronized: capture runs concurrently, and {@link #drainAndReset()} must never lose a capture that lands mid-drain. */
public final class ConsumerRecomputeBuffer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerRecomputeBuffer.class);

    private static final int DEFAULT_HARD_CAP = 10_000;

    private final Set<String> taskIds = new HashSet<>();
    private final Set<String> touchedGroupIds = new HashSet<>();
    private final Set<String> namedUserIds = new HashSet<>();
    private final Set<String> processInstanceIds = new HashSet<>();
    private final Set<String> namedInitiatorIds = new HashSet<>();
    private final int hardCap;
    private Instant windowStartedAt;

    public ConsumerRecomputeBuffer() {
        this(DEFAULT_HARD_CAP);
    }

    /** @param hardCap total touched tasks + processes above which further captures are dropped. */
    public ConsumerRecomputeBuffer(int hardCap) {
        this.hardCap = hardCap;
    }

    /** Records a touched task and any directly-named users (assignee, owner, completedBy). */
    public synchronized void captureTask(String taskId, Instant at, String... namedUsers) {
        if (taskId == null) {
            return;
        }
        if (!taskIds.contains(taskId) && atCapacity()) {
            LOGGER.warn("Recompute buffer at hard cap ({}); dropping capture for task {}", hardCap, taskId);
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
        if (!taskIds.contains(taskId) && atCapacity()) {
            LOGGER.warn(
                "Recompute buffer at hard cap ({}); dropping candidate-group capture for task {}",
                hardCap,
                taskId
            );
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
        if (!processInstanceIds.contains(processInstanceId) && atCapacity()) {
            LOGGER.warn(
                "Recompute buffer at hard cap ({}); dropping capture for process {}",
                hardCap,
                processInstanceId
            );
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

    /** Count of distinct touched tasks and processes. */
    public synchronized int size() {
        return taskIds.size() + processInstanceIds.size();
    }

    /** Time since the first capture of the current window. */
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

    /**
     * Re-adds a drained window's identities after a failed flush, so a transient failure loses
     * nothing - unless the buffer is already at capacity, in which case the whole window is
     * dropped rather than pushing further past the hard cap.
     */
    public synchronized void mergeBack(ConsumerRecomputeWindow window, Instant at) {
        if (window.isEmpty()) {
            return;
        }
        if (atCapacity()) {
            LOGGER.warn("Recompute buffer at hard cap ({}); dropping a failed window on merge-back", hardCap);
            return;
        }
        markTouch(at);
        taskIds.addAll(window.taskIds());
        touchedGroupIds.addAll(window.touchedGroupIds());
        namedUserIds.addAll(window.namedUserIds());
        processInstanceIds.addAll(window.processInstanceIds());
        namedInitiatorIds.addAll(window.namedInitiatorIds());
    }

    private boolean atCapacity() {
        return size() >= hardCap;
    }

    private void markTouch(Instant at) {
        if (windowStartedAt == null) {
            windowStartedAt = at;
        }
    }
}
