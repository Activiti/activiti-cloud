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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PushedCountsRecomputeBufferTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final PushedCountsRecomputeBuffer buffer = new PushedCountsRecomputeBuffer();

    @Test
    void isEmpty_whenNothingCaptured() {
        assertThat(buffer.isEmpty()).isTrue();
        assertThat(buffer.size()).isZero();
    }

    @Test
    void captureTask_recordsTheTaskAndItsNamedUsers() {
        buffer.captureTask("task-1", T0, "alice", "bob");

        PushedCountsRecomputeWindow window = buffer.drainAndReset();

        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.namedUserIds()).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void captureTask_ignoresBlankOrNullNamedUsers() {
        buffer.captureTask("task-1", T0, "alice", null, "", "  ");

        assertThat(buffer.drainAndReset().namedUserIds()).containsExactly("alice");
    }

    @Test
    void captureTask_withNullTaskId_isIgnored() {
        buffer.captureTask(null, T0, "alice");

        assertThat(buffer.isEmpty()).isTrue();
    }

    @Test
    void captureTaskCandidateGroup_recordsTheTaskAndTheGroup() {
        buffer.captureTaskCandidateGroup("task-1", "eng", T0);

        PushedCountsRecomputeWindow window = buffer.drainAndReset();

        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.touchedGroupIds()).containsExactly("eng");
    }

    @Test
    void captureProcess_recordsTheProcessAndItsInitiator() {
        buffer.captureProcess("proc-1", "alice", T0);

        PushedCountsRecomputeWindow window = buffer.drainAndReset();

        assertThat(window.processInstanceIds()).containsExactly("proc-1");
        assertThat(window.namedInitiatorIds()).containsExactly("alice");
    }

    @Test
    void captureProcess_withNoInitiator_recordsOnlyTheProcess() {
        buffer.captureProcess("proc-1", null, T0);

        PushedCountsRecomputeWindow window = buffer.drainAndReset();

        assertThat(window.processInstanceIds()).containsExactly("proc-1");
        assertThat(window.namedInitiatorIds()).isEmpty();
    }

    @Test
    void size_countsDistinctTasksAndProcesses() {
        buffer.captureTask("task-1", T0);
        buffer.captureTask("task-1", T0);
        buffer.captureTask("task-2", T0);
        buffer.captureProcess("proc-1", null, T0);

        assertThat(buffer.size()).isEqualTo(3);
    }

    @Test
    void age_isZero_whenNothingCapturedYet() {
        Clock clock = Clock.fixed(T0, ZoneOffset.UTC);

        assertThat(buffer.age(clock)).isEqualTo(Duration.ZERO);
    }

    @Test
    void age_measuresFromTheFirstCaptureOfTheWindow_notTheLatest() {
        buffer.captureTask("task-1", T0);
        buffer.captureTask("task-2", T0.plusSeconds(1));

        Clock clock = Clock.fixed(T0.plusSeconds(2), ZoneOffset.UTC);

        assertThat(buffer.age(clock)).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void mergeBack_restoresADrainedWindow() {
        buffer.captureTask("task-1", T0, "alice");
        PushedCountsRecomputeWindow window = buffer.drainAndReset();

        buffer.mergeBack(window, T0.plusSeconds(1));

        PushedCountsRecomputeWindow restored = buffer.drainAndReset();
        assertThat(restored.taskIds()).containsExactly("task-1");
        assertThat(restored.namedUserIds()).containsExactly("alice");
    }

    @Test
    void mergeBack_ofAnEmptyWindow_doesNothing() {
        buffer.mergeBack(new PushedCountsRecomputeWindow(Set.of(), Set.of(), Set.of(), Set.of(), Set.of()), T0);

        assertThat(buffer.isEmpty()).isTrue();
    }

    @Test
    void drainAndReset_clearsTheBuffer_andStartsAFreshWindow() {
        buffer.captureTask("task-1", T0, "alice");
        buffer.drainAndReset();

        assertThat(buffer.isEmpty()).isTrue();
        assertThat(buffer.age(Clock.fixed(T0.plusSeconds(10), ZoneOffset.UTC))).isEqualTo(Duration.ZERO);
    }

    @Test
    void drainAndReset_underConcurrentCapture_neverLosesAnEntry() throws InterruptedException {
        int threads = 16;
        int perThread = 200;
        Set<String> allCaptured;
        Set<String> drained;
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            allCaptured = ConcurrentHashMap.newKeySet();
            drained = ConcurrentHashMap.newKeySet();

            for (int t = 0; t < threads; t++) {
                int threadIndex = t;
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                    for (int i = 0; i < perThread; i++) {
                        String taskId = "task-" + threadIndex + "-" + i;
                        allCaptured.add(taskId);
                        buffer.captureTask(taskId, T0);
                        if (i % 10 == 0) {
                            drained.addAll(buffer.drainAndReset().taskIds());
                        }
                    }
                });
            }

            ready.await();
            go.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
        drained.addAll(buffer.drainAndReset().taskIds());

        assertThat(drained).isEqualTo(allCaptured);
    }
}
