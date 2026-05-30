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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pressure tests that validate {@link FunctionRouterExecutorFactory} behaviour under load for
 * concurrency levels 2, 5 and 10.
 *
 * <p>Each test submits a large batch of tasks to a single registration key and verifies:
 * <ul>
 *   <li>Exactly {@code concurrency} threads are used (the pool is fully exercised).</li>
 *   <li>Every submitted task completes successfully (no errors, no lost work).</li>
 *   <li>Parallel execution is faster than a naive single-threaded estimate.</li>
 * </ul>
 *
 * <p>The second parameterised test additionally verifies that each registration key receives its
 * own isolated thread pool and that threads are never shared between keys.
 *
 * <p>The third test ({@code shouldReportThroughputGrowthWith10kTasksAtVariousConcurrencyLevels})
 * fixes the ingestion volume at {@value #INGESTION_TOTAL_TASKS} tasks and varies the concurrency
 * level. It logs elapsed time and throughput for each run so the scaling behaviour can be
 * analysed without asserting specific timing values.
 */
class FunctionRouterConcurrencyPressureTest {

    private static final Logger log = LoggerFactory.getLogger(FunctionRouterConcurrencyPressureTest.class);

    /** Fixed task count used by the ingestion quantity test. */
    private static final int INGESTION_TOTAL_TASKS = 10_000;

    private FunctionRouterExecutorFactory factory;

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.destroy();
        }
    }

    /**
     * Submits {@code totalTasks} tasks to a single registration key executor and asserts that
     * exactly {@code concurrency} threads are used, all tasks complete, and parallel execution
     * beats the single-threaded time estimate.
     */
    @ParameterizedTest(name = "concurrency={0}, totalTasks={1}")
    @CsvSource({
        "2,  100",
        "5,  200",
        "10, 500",
    })
    void shouldProcessTasksConcurrentlyWithConfiguredThreadCount(int concurrency, int totalTasks)
        throws InterruptedException {
        // Given
        factory = new FunctionRouterExecutorFactory();
        factory.setConcurrency(concurrency);
        factory.setTimeout(Duration.ofSeconds(30));

        final var executor = factory.apply("pressure-test-key");
        final Set<Thread> observedThreads = ConcurrentHashMap.newKeySet();
        final var completedCount = new AtomicInteger(0);
        final var errorCount = new AtomicInteger(0);

        final long startNanos = System.nanoTime();

        // When – submit all tasks sequentially; the calling thread blocks under backpressure
        // whenever both pool threads are busy and the single-slot queue is full.
        for (int i = 0; i < totalTasks; i++) {
            executor.submit(() -> {
                try {
                    observedThreads.add(Thread.currentThread());
                    // Sleep long enough that all pool threads engage simultaneously,
                    // ensuring parallelism is observable over the full run.
                    Thread.sleep(5);
                    completedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errorCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS))
            .as("All tasks should complete within the 30 s timeout")
            .isTrue();

        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        // Then
        assertThat(completedCount.get())
            .as("All %d tasks must complete successfully", totalTasks)
            .isEqualTo(totalTasks);

        assertThat(errorCount.get())
            .as("No task should fail with an error")
            .isEqualTo(0);

        assertThat(observedThreads.size())
            .as("Exactly %d thread(s) should be used when concurrency=%d", concurrency, concurrency)
            .isEqualTo(concurrency);

        // Performance: each task sleeps 5 ms, so a single thread would need roughly
        // totalTasks × 5 ms.  With N concurrent threads the wall-clock time should be
        // well below that estimate (target ≈ totalTasks × 5 / N ms).
        final long singleThreadedEstimateMs = (long) totalTasks * 5;
        assertThat(elapsedMillis)
            .as(
                "Parallel execution (concurrency=%d) should complete in less than the single-threaded estimate of %d ms",
                concurrency,
                singleThreadedEstimateMs
            )
            .isLessThan(singleThreadedEstimateMs);
    }

    /**
     * Creates {@code numRegistrationKeys} independent executors and submits {@code totalTasks}
     * tasks to each. Verifies that every key uses exactly {@code concurrency} threads and that
     * no thread is shared between different registration keys.
     */
    @ParameterizedTest(name = "concurrency={0}, totalTasks={1}, keys={2}")
    @CsvSource({
        "2,  100, 3",
        "5,  200, 5",
        "10, 500, 10",
    })
    void shouldIsolateThreadPoolsPerRegistrationKey(int concurrency, int totalTasks, int numRegistrationKeys)
        throws InterruptedException {
        // Given
        factory = new FunctionRouterExecutorFactory();
        factory.setConcurrency(concurrency);
        factory.setTimeout(Duration.ofSeconds(60));

        final var threadsPerKey = new ConcurrentHashMap<String, Set<Thread>>();
        final var completedCount = new AtomicInteger(0);

        // When – submit tasks for every registration key sequentially
        for (int k = 0; k < numRegistrationKeys; k++) {
            final String key = "registration-key-" + k;
            final var executor = factory.apply(key);
            threadsPerKey.put(key, ConcurrentHashMap.newKeySet());

            for (int i = 0; i < totalTasks; i++) {
                executor.submit(() -> {
                    threadsPerKey.get(key).add(Thread.currentThread());
                    completedCount.incrementAndGet();
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }

        factory.shutdown();
        assertThat(factory.awaitTermination(60, TimeUnit.SECONDS))
            .as("All tasks for all registration keys should complete within the 60 s timeout")
            .isTrue();

        // Then – every task completed
        assertThat(completedCount.get())
            .as("%d keys x %d tasks = %d total tasks must all complete", numRegistrationKeys, totalTasks, numRegistrationKeys * totalTasks)
            .isEqualTo(numRegistrationKeys * totalTasks);

        // Each key's pool uses exactly concurrency threads
        threadsPerKey.forEach((key, threads) ->
            assertThat(threads.size())
                .as("Registration key '%s' should use exactly %d thread(s)", key, concurrency)
                .isEqualTo(concurrency)
        );

        // Thread pools are fully isolated: no thread appears in more than one key's set
        final long totalDistinctThreads = threadsPerKey
            .values()
            .stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet())
            .size();

        assertThat(totalDistinctThreads)
            .as(
                "Thread pools must be isolated: %d keys x %d threads each = %d distinct threads expected",
                numRegistrationKeys,
                concurrency,
                numRegistrationKeys * concurrency
            )
            .isEqualTo((long) numRegistrationKeys * concurrency);
    }

    /**
     * Ingestion quantity test: always submits exactly {@value #INGESTION_TOTAL_TASKS} tasks and
     * measures how wall-clock time changes as concurrency grows.
     *
     * <p>Each task parks for ~0.1 ms ({@code LockSupport.parkNanos(100_000)}) to simulate a
     * realistic unit of work and to make thread-level parallelism visible in the measurements.
     * Results are printed via the logger so they appear in the test report; the only hard
     * assertions are correctness checks (all tasks complete, zero errors).
     *
     * <p>Example output (results vary by hardware):
     * <pre>
     * [ingestion] concurrency=  1 | tasks=10000 | elapsed= 1050ms | throughput=  9523 tasks/s
     * [ingestion] concurrency=  5 | tasks=10000 | elapsed=  215ms | throughput= 46511 tasks/s
     * [ingestion] concurrency= 10 | tasks=10000 | elapsed=  113ms | throughput= 88495 tasks/s
     * [ingestion] concurrency= 25 | tasks=10000 | elapsed=   52ms | throughput=192307 tasks/s
     * [ingestion] concurrency= 50 | tasks=10000 | elapsed=   31ms | throughput=322580 tasks/s
     * [ingestion] concurrency=100 | tasks=10000 | elapsed=   22ms | throughput=454545 tasks/s
     * </pre>
     */
    @ParameterizedTest(name = "concurrency={0}")
    @ValueSource(ints = { 1, 5, 10, 25, 50, 100 })
    void shouldReportThroughputGrowthWith10kTasksAtVariousConcurrencyLevels(int concurrency)
        throws InterruptedException {
        // Given
        factory = new FunctionRouterExecutorFactory();
        factory.setConcurrency(concurrency);
        factory.setTimeout(Duration.ofSeconds(120));

        final var executor = factory.apply("ingestion-test-key");
        final var completedCount = new AtomicInteger(0);
        final var errorCount = new AtomicInteger(0);

        final long startNanos = System.nanoTime();

        // When – submit all 10 000 tasks; the calling thread blocks under backpressure
        // whenever all pool threads are busy and the single-slot queue is full.
        for (int i = 0; i < INGESTION_TOTAL_TASKS; i++) {
            executor.submit(() -> {
                try {
                    // ~0.1 ms synthetic workload so that parallelism is the dominant factor.
                    LockSupport.parkNanos(100_000L);
                    completedCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(120, TimeUnit.SECONDS))
            .as("All %d tasks should complete within the 120 s timeout (concurrency=%d)", INGESTION_TOTAL_TASKS, concurrency)
            .isTrue();

        final long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        final long throughput = INGESTION_TOTAL_TASKS * 1000L / Math.max(elapsedMs, 1);

        // Then – correctness: every task must have finished without error
        assertThat(completedCount.get())
            .as("All %d tasks must complete successfully (concurrency=%d)", INGESTION_TOTAL_TASKS, concurrency)
            .isEqualTo(INGESTION_TOTAL_TASKS);

        assertThat(errorCount.get())
            .as("No task should fail with an error (concurrency=%d)", concurrency)
            .isEqualTo(0);

        // Report timing so the scaling behaviour is visible in the test output
        log.info(
            "[ingestion] concurrency={} | tasks={} | elapsed={}ms | throughput={} tasks/s",
            String.format("%3d", concurrency),
            INGESTION_TOTAL_TASKS,
            String.format("%6d", elapsedMs),
            String.format("%7d", throughput)
        );
    }
}
