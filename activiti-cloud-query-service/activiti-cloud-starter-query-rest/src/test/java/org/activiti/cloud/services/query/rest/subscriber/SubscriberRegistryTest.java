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
package org.activiti.cloud.services.query.rest.subscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SubscriberRegistryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SubscriberRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SubscriberRegistry(eventPublisher, 50_000);
    }

    @Test
    void should_publishWentLiveEvent_when_firstSessionForAUserIsRegistered() {
        registry.register("alice", Set.of("eng"), "session-1", NOW);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(SubscriberWentLiveEvent.class);
        assertThat(((SubscriberWentLiveEvent) event.getValue()).userId()).isEqualTo("alice");
    }

    @Test
    void should_notPublishWentLiveEvent_when_secondSessionForTheSameUserIsRegistered() {
        registry.register("alice", Set.of("eng"), "session-1", NOW);
        clearInvocations(eventPublisher);

        registry.register("alice", Set.of("eng"), "session-2", NOW);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_publishWentQuietEvent_when_theLastSessionForAUserIsUnregistered() {
        registry.register("alice", Set.of("eng"), "session-1", NOW);
        clearInvocations(eventPublisher);

        registry.unregister("alice", "session-1", NOW);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(SubscriberWentQuietEvent.class);
        assertThat(registry.size()).isZero();
    }

    @Test
    void should_notPublishWentQuietEvent_when_oneOfTwoSessionsIsUnregistered() {
        registry.register("alice", Set.of("eng"), "session-1", NOW);
        registry.register("alice", Set.of("eng"), "session-2", NOW);
        clearInvocations(eventPublisher);

        registry.unregister("alice", "session-1", NOW);

        verifyNoInteractions(eventPublisher);
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void should_beANoOp_when_unregisteringAUserWithNoRegistration() {
        registry.unregister("nobody", "session-1", NOW);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_notRegister_when_registryIsAtItsConfiguredMaximumSize() {
        SubscriberRegistry smallRegistry = new SubscriberRegistry(eventPublisher, 1);
        smallRegistry.register("alice", Set.of(), "session-1", NOW);
        clearInvocations(eventPublisher);

        smallRegistry.register("bob", Set.of(), "session-1", NOW);

        verify(eventPublisher, never()).publishEvent(any());
        assertThat(smallRegistry.size()).isEqualTo(1);
    }

    @Test
    void should_stillAllowAnAdditionalSession_when_userIsAlreadyRegisteredAtMaximumSize() {
        SubscriberRegistry smallRegistry = new SubscriberRegistry(eventPublisher, 1);
        smallRegistry.register("alice", Set.of(), "session-1", NOW);

        smallRegistry.register("alice", Set.of(), "session-2", NOW);

        assertThat(smallRegistry.size()).isEqualTo(1);
    }

    @Test
    void should_produceExactlyOneWentLiveEvent_when_manyThreadsRegisterSessionsForTheSameUserConcurrently()
        throws InterruptedException {
        int threadCount = 32;
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                int sessionIndex = i;
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        registry.register("alice", Set.of("eng"), "session-" + sessionIndex, NOW);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
        }

        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(events.capture());
        assertThat(events.getAllValues()).hasSize(1).first().isInstanceOf(SubscriberWentLiveEvent.class);
        assertThat(registry.size()).isEqualTo(1);
    }
}
