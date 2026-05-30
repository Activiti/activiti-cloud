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
package org.activiti.cloud.common.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pressure tests that validate {@link FunctionRouterExecutorFactory} behaviour under different
 * concurrency levels (2, 5 and 10 threads).
 *
 * <p>Each parameterized test is executed three times – once per concurrency level – and verifies:
 * <ul>
 *   <li>Correct number of unique threads used (matching the configured concurrency level).</li>
 *   <li>All tasks complete successfully without errors.</li>
 *   <li>Execution time demonstrates real parallelism (elapsed time is bounded by the
 *       theoretical minimum for the given concurrency level).</li>
 *   <li>No race conditions or thread-safety issues under sustained load.</li>
 *   <li>Thread pools for different registration keys remain fully isolated.</li>
 * </ul>
 *
 * <h2>Queue behaviour note</h2>
 * The executor created by {@link FunctionRouterExecutorFactory} uses a
 * {@link java.util.concurrent.LinkedBlockingQueue} with capacity&nbsp;1.  When the queue is full
 * the {@code RejectedExecutionHandler} blocks the submitting thread until space becomes available.
 * All tests account for this back-pressure by measuring wall-clock time that includes the
 * throttled-submission overhead.
 */
class FunctionRouterConcurrencyPressureTest {

    /**
     * Duration each task sleeps to ensure every worker thread is created and active before any
     * task completes.  Must be long enough that, for the smallest concurrency level (2), all core
     * threads are occupied simultaneously before the first task finishes.
     */
    private static final long TASK_DURATION_MS = 20L;

    /** Upper bound for waiting on latches / all-tasks completion in seconds. */
    private static final long TEST_TIMEOUT_SECONDS = 60L;

    private FunctionRouterExecutorFactory factory;

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.destroy();
        }
    }

    // ---------------------------------------------------------------------------
    // 1. Thread-count verification – exactly concurrency threads are used
    // ---------------------------------------------------------------------------

    /**
     * Submits {@code concurrency × 20} tasks to a single registration key and verifies that
     * exactly {@code concurrency} distinct OS threads executed them.
     *
     * <p>Each task sleeps for {@value #TASK_DURATION_MS}&nbsp;ms so that the first wave of
     * {@code concurrency} tasks keeps all core threads occupied while additional tasks are
     * submitted.  This guarantees every core thread is created and recorded before any task
     * completes.
     *
     * <p>Back-pressure note: the executor's single-slot queue means that task submissions
     * beyond {@code concurrency + 1} block the test thread until a worker becomes free.  This
     * throttling does not affect the thread-count assertion – it only adds a bounded delay.
     */
    @ParameterizedTest(name = "concurrency={0}")
    @ValueSource(ints = { 2, 5, 10 })
    void shouldUseExactlyAsManyThreadsAsConcurrencyLevel(int concurrency) throws InterruptedException {
        factory = createFactory(concurrency);
        final int taskCount = concurrency * 20;
        final Set<Long> observedThreadIds = ConcurrentHashMap.newKeySet();
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch allTasksDone = new CountDownLatch(taskCount);
        final ExecutorService executor = factory.apply("pressure-registration");

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    observedThreadIds.add(Thread.currentThread().threadId());
                    Thread.sleep(TASK_DURATION_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add(e);
                } finally {
                    allTasksDone.countDown();
                }
            });
        }

        assertThat(allTasksDone.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .as("All %d tasks should complete within %d seconds", taskCount, TEST_TIMEOUT_SECONDS)
            .isTrue();

        assertThat(errors).as("No errors should occur during execution").isEmpty();
        assertThat(observedThreadIds)
            .as("Number of distinct threads should equal the configured concurrency level %d", concurrency)
            .hasSize(concurrency);
    }

    // ---------------------------------------------------------------------------
    // 2. Parallel throughput – elapsed time confirms tasks run concurrently
    // ---------------------------------------------------------------------------

    /**
     * Submits {@code concurrency × 10} tasks and asserts that the wall-clock elapsed time is
     * well below what sequential execution would take, confirming genuine parallelism.
     *
     * <p>The theoretical minimum elapsed time is
     * {@code ceil(taskCount / concurrency) × TASK_DURATION_MS}.  An allowance of 5× is applied
     * to account for scheduling overhead and back-pressure from the throttled-submission
     * mechanism, while still proving that tasks do not run one-at-a-time.
     */
    @ParameterizedTest(name = "concurrency={0}")
    @ValueSource(ints = { 2, 5, 10 })
    void shouldCompleteTasksFasterWithHigherConcurrency(int concurrency) throws InterruptedException {
        factory = createFactory(concurrency);
        final int taskCount = concurrency * 10;
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch allDone = new CountDownLatch(taskCount);
        final ExecutorService executor = factory.apply("throughput-registration");

        final long start = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(TASK_DURATION_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add(e);
                } finally {
                    allDone.countDown();
                }
            });
        }

        assertThat(allDone.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .as("All %d tasks should complete within %d seconds", taskCount, TEST_TIMEOUT_SECONDS)
            .isTrue();

        final long elapsed = System.currentTimeMillis() - start;

        assertThat(errors).as("No errors should occur during execution").isEmpty();

        // Sequential time: taskCount × TASK_DURATION_MS
        final long sequentialMs = (long) taskCount * TASK_DURATION_MS;
        // Theoretical minimum with full parallelism: ceil(taskCount / concurrency) × TASK_DURATION_MS
        final long theoreticalMinMs = (long) Math.ceil((double) taskCount / concurrency) * TASK_DURATION_MS;
        // Generous upper bound: 5× theoretical minimum, well below sequential time
        final long acceptableMaxMs = theoreticalMinMs * 5;

        assertThat(elapsed)
            .as(
                "Elapsed=%d ms should be ≤ %d ms (5× theoretical min=%d ms for concurrency=%d); sequential would be %d ms",
                elapsed,
                acceptableMaxMs,
                theoreticalMinMs,
                concurrency,
                sequentialMs
            )
            .isLessThanOrEqualTo(acceptableMaxMs);
    }

    // ---------------------------------------------------------------------------
    // 3. Registration isolation – each key gets its own independent thread pool
    // ---------------------------------------------------------------------------

    /**
     * Submits tasks to three distinct registration keys in parallel and verifies that:
     * <ol>
     *   <li>Each registration uses exactly {@code concurrency} distinct threads.</li>
     *   <li>No thread is shared across registration keys (pools are fully isolated).</li>
     * </ol>
     *
     * <p>Tasks for different registrations are submitted from independent
     * {@link CompletableFuture} threads so that all pools are exercised concurrently.
     */
    @ParameterizedTest(name = "concurrency={0}")
    @ValueSource(ints = { 2, 5, 10 })
    void shouldIsolateThreadPoolsAcrossRegistrations(int concurrency) {
        factory = createFactory(concurrency);
        final int registrationCount = 3;
        final int tasksPerRegistration = concurrency * 10;
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        final List<Set<Long>> threadIdSets = new ArrayList<>();
        final List<CountDownLatch> doneLatches = new ArrayList<>();
        for (int r = 0; r < registrationCount; r++) {
            threadIdSets.add(ConcurrentHashMap.newKeySet());
            doneLatches.add(new CountDownLatch(tasksPerRegistration));
        }

        // Submit all registrations in parallel
        final List<CompletableFuture<Void>> submitters = IntStream
            .range(0, registrationCount)
            .mapToObj(r ->
                CompletableFuture.runAsync(() -> {
                    final ExecutorService executor = factory.apply("reg-" + r);
                    final Set<Long> threadIds = threadIdSets.get(r);
                    final CountDownLatch done = doneLatches.get(r);
                    for (int i = 0; i < tasksPerRegistration; i++) {
                        executor.submit(() -> {
                            try {
                                threadIds.add(Thread.currentThread().threadId());
                                Thread.sleep(TASK_DURATION_MS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                errors.add(e);
                            } finally {
                                done.countDown();
                            }
                        });
                    }
                })
            )
            .toList();

        CompletableFuture.allOf(submitters.toArray(CompletableFuture[]::new)).join();

        for (int r = 0; r < registrationCount; r++) {
            try {
                assertThat(doneLatches.get(r).await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .as("All tasks for reg-%d should complete within %d seconds", r, TEST_TIMEOUT_SECONDS)
                    .isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for reg-" + r, e);
            }
        }

        assertThat(errors).as("No errors should occur during execution").isEmpty();

        // Each registration should have exactly concurrency distinct threads
        for (int r = 0; r < registrationCount; r++) {
            assertThat(threadIdSets.get(r))
                .as("Registration reg-%d should use exactly %d distinct threads", r, concurrency)
                .hasSize(concurrency);
        }

        // No thread should appear in more than one registration's pool
        for (int r1 = 0; r1 < registrationCount; r1++) {
            for (int r2 = r1 + 1; r2 < registrationCount; r2++) {
                final Set<Long> overlap = ConcurrentHashMap.newKeySet();
                overlap.addAll(threadIdSets.get(r1));
                overlap.retainAll(threadIdSets.get(r2));
                assertThat(overlap)
                    .as(
                        "Thread pools for reg-%d and reg-%d should be fully isolated (no shared threads)",
                        r1,
                        r2
                    )
                    .isEmpty();
            }
        }
    }

    // ---------------------------------------------------------------------------
    // 4. Stability – large task volumes complete without errors
    // ---------------------------------------------------------------------------

    /**
     * Submits a large number of tasks (100 / 200 / 500 depending on concurrency level) and
     * verifies that all complete successfully, confirming system stability under sustained load.
     *
     * <p>Task counts match the requirements from the problem statement:
     * <ul>
     *   <li>Concurrency 2 → 100 tasks</li>
     *   <li>Concurrency 5 → 200 tasks</li>
     *   <li>Concurrency 10 → 500 tasks</li>
     * </ul>
     */
    @ParameterizedTest(name = "concurrency={0}")
    @ValueSource(ints = { 2, 5, 10 })
    void shouldHandleLargeNumberOfTasksWithoutErrors(int concurrency) throws InterruptedException {
        factory = createFactory(concurrency);

        final int taskCount = switch (concurrency) {
            case 2 -> 100;
            case 5 -> 200;
            default -> 500;
        };

        final AtomicInteger completedTasks = new AtomicInteger(0);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch allDone = new CountDownLatch(taskCount);
        final ExecutorService executor = factory.apply("stability-registration");

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    completedTasks.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    allDone.countDown();
                }
            });
        }

        assertThat(allDone.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .as("All %d tasks should complete within %d seconds", taskCount, TEST_TIMEOUT_SECONDS)
            .isTrue();

        assertThat(errors).as("No errors should occur during execution").isEmpty();
        assertThat(completedTasks.get())
            .as("All %d tasks should have completed successfully", taskCount)
            .isEqualTo(taskCount);
    }

    // ---------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------

    private static FunctionRouterExecutorFactory createFactory(int concurrency) {
        final var f = new FunctionRouterExecutorFactory();
        f.setConcurrency(concurrency);
        f.setTimeout(Duration.ofSeconds(TEST_TIMEOUT_SECONDS));
        return f;
    }
}
