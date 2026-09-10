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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Exercises the {@code synchronized} registry under concurrent multi-instance delivery. */
class ConsumerSubscriberRegistryConcurrencyTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void concurrentRegistersFromDistinctSources_areAllRetained() throws InterruptedException {
        ConsumerSubscriberRegistry registry = new ConsumerSubscriberRegistry();
        int instances = 16;
        ExecutorService pool = Executors.newFixedThreadPool(instances);

        try {
            // All instances first-register the same user at once: the put + sources.add compound must not lose any.
            CountDownLatch registered = submitEach(pool, instances, source ->
                registry.register("alice", List.of("eng"), source, T0)
            );
            assertThat(registered.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(registry.isWatching("alice")).isTrue();
        assertThat(registry.sourcesOf("alice")).hasSize(instances);
        assertThat(registry.groupsOf("alice")).containsExactly("eng");
    }

    @Test
    void userSurvivesUntilItsLastHolderUnregisters_underConcurrency() throws InterruptedException {
        ConsumerSubscriberRegistry registry = new ConsumerSubscriberRegistry();
        int instances = 16;
        String lastSource = "rest-" + (instances - 1);
        ExecutorService pool = Executors.newFixedThreadPool(instances);

        try {
            CountDownLatch registered = submitEach(pool, instances, source ->
                registry.register("alice", List.of("eng"), source, T0)
            );
            assertThat(registered.await(30, TimeUnit.SECONDS)).isTrue();
            assertThat(registry.sourcesOf("alice")).hasSize(instances);

            // Concurrently drop every holder but the last; the user must stay watched throughout.
            CountDownLatch allButLastRemoved = submitEach(pool, instances - 1, source ->
                registry.unregister("alice", source)
            );
            assertThat(allButLastRemoved.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(registry.isWatching("alice")).isTrue();
        assertThat(registry.sourcesOf("alice")).containsExactly(lastSource);

        registry.unregister("alice", lastSource);
        assertThat(registry.isWatching("alice")).isFalse();
    }

    private static CountDownLatch submitEach(ExecutorService pool, int instances, Consumer<String> action) {
        CountDownLatch latch = new CountDownLatch(instances);
        for (int i = 0; i < instances; i++) {
            String source = "rest-" + i;
            pool.submit(() -> {
                try {
                    action.accept(source);
                } finally {
                    latch.countDown();
                }
            });
        }
        return latch;
    }
}
