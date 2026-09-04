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
import org.junit.jupiter.api.Test;

/** Exercises the {@code synchronized} registry under concurrent multi-instance delivery. */
class ConsumerSubscriberRegistryConcurrencyTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void concurrentRegisterAndUnregister_leaveTheRegistryConsistent() throws InterruptedException {
        ConsumerSubscriberRegistry registry = new ConsumerSubscriberRegistry();
        int instances = 8;
        int iterations = 2000;
        ExecutorService pool = Executors.newFixedThreadPool(instances);
        CountDownLatch done = new CountDownLatch(instances);

        for (int i = 0; i < instances; i++) {
            String source = "rest-" + i;
            pool.submit(() -> {
                try {
                    for (int n = 0; n < iterations; n++) {
                        registry.register("alice", List.of("eng"), source, T0);
                        registry.unregister("alice", source);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // each source registered and unregistered in equal measure, ending on unregister => user gone
        assertThat(registry.isWatching("alice")).isFalse();
        assertThat(registry.watchedUserIds()).isEmpty();
    }

    @Test
    void concurrentRegisterFromManyInstances_thenUnregisterEach_dropsUserOnlyAfterTheLast()
        throws InterruptedException {
        ConsumerSubscriberRegistry registry = new ConsumerSubscriberRegistry();
        int instances = 16;
        ExecutorService pool = Executors.newFixedThreadPool(instances);

        CountDownLatch registered = submitEach(pool, instances, source ->
            registry.register("alice", List.of("eng"), source, T0)
        );
        assertThat(registered.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(registry.sourcesOf("alice")).hasSize(instances);

        CountDownLatch unregistered = submitEach(pool, instances, source -> registry.unregister("alice", source));
        assertThat(unregistered.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(registry.isWatching("alice")).isFalse();
    }

    private static CountDownLatch submitEach(
        ExecutorService pool,
        int instances,
        java.util.function.Consumer<String> action
    ) {
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
