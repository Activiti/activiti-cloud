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

package org.activiti.cloud.common.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class OutboxLifecycleReconcilerTest {

    private OutboxProperties outboxProperties;
    private ConcurrentMetadataStore outboxMetadataStore;

    @Mock
    private OutboxLifecycleController outboxLifecycleController;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private OutboxLifecycleReconciler reconciler;

    @BeforeEach
    void setUp() {
        outboxProperties = new OutboxProperties();
        outboxProperties.setReconcilePeriod(Duration.ofSeconds(5));
        outboxMetadataStore = new SimpleMetadataStore();

        reconciler = new OutboxLifecycleReconciler(
            outboxProperties,
            outboxMetadataStore,
            outboxLifecycleController,
            taskScheduler
        );
    }

    @Nested
    @DisplayName("reconcile()")
    class ReconcileTests {

        private enum ExpectedAction {
            START,
            STOP,
            NONE,
        }

        @ParameterizedTest(name = "isRunning={0}, desiredState={1} -> {2}")
        @CsvSource(
            nullValues = "NULL",
            textBlock = """
            # isRunning, desiredState, expectedAction
            true,        false,        STOP
            true,        FALSE,        STOP
            true,        true,         NONE
            true,        TRUE,         NONE
            true,        '',           NONE
            true,        NULL,         NONE
            true,        invalid,      NONE
            false,       false,        NONE
            false,       FALSE,        NONE
            false,       true,         START
            false,       TRUE,         START
            false,       '',           START
            false,       NULL,         START
            false,       invalid,      START
            """
        )
        @DisplayName("reconciles endpoint state based on running status and metadata store value")
        void reconcilesEndpointState(boolean isRunning, String desiredState, ExpectedAction expectedAction) {
            String endpoint = "partition-1";
            when(outboxLifecycleController.getEndpointsRunningStatus()).thenReturn(Map.of(endpoint, isRunning));

            if (desiredState != null) {
                outboxMetadataStore.put(endpoint, desiredState);
            }

            reconciler.reconcile();

            switch (expectedAction) {
                case START -> {
                    verify(outboxLifecycleController).start(endpoint);
                    verify(outboxLifecycleController, never()).stop(any());
                }
                case STOP -> {
                    verify(outboxLifecycleController).stop(endpoint);
                    verify(outboxLifecycleController, never()).start(any());
                }
                case NONE -> {
                    verify(outboxLifecycleController, never()).start(any());
                    verify(outboxLifecycleController, never()).stop(any());
                }
            }
        }
    }

    @Nested
    @DisplayName("setAutoStartup()")
    class SetAutoStartupTests {

        @Test
        @DisplayName("puts 'true' in metadata store for all endpoints when autoStartup is true")
        void putsTrueWhenAutoStartupIsTrue() {
            when(outboxLifecycleController.getEndpoints()).thenReturn(List.of("partition-1", "partition-2"));

            reconciler.setAutoStartup(true);

            assertThat(outboxMetadataStore.get("partition-1")).isEqualTo("true");
            assertThat(outboxMetadataStore.get("partition-2")).isEqualTo("true");
        }

        @Test
        @DisplayName("puts 'false' in metadata store for all endpoints when autoStartup is false")
        void putsFalseWhenAutoStartupIsFalse() {
            when(outboxLifecycleController.getEndpoints()).thenReturn(List.of("partition-1", "partition-2"));

            reconciler.setAutoStartup(false);

            assertThat(outboxMetadataStore.get("partition-1")).isEqualTo("false");
            assertThat(outboxMetadataStore.get("partition-2")).isEqualTo("false");
        }

        @Test
        @DisplayName("removes endpoints from metadata store when autoStartup is null")
        void removesKeysWhenAutoStartupIsNull() {
            outboxMetadataStore.put("partition-1", "true");
            outboxMetadataStore.put("partition-2", "false");
            when(outboxLifecycleController.getEndpoints()).thenReturn(List.of("partition-1", "partition-2"));

            reconciler.setAutoStartup(null);

            assertThat(outboxMetadataStore.get("partition-1")).isNull();
            assertThat(outboxMetadataStore.get("partition-2")).isNull();
        }
    }

    @Nested
    @DisplayName("SmartLifecycle: start(), stop(), isRunning()")
    class LifecycleTests {

        @Test
        @DisplayName("isRunning() returns false initially")
        void isRunningReturnsFalseInitially() {
            assertThat(reconciler.isRunning()).isFalse();
        }

        @Test
        @DisplayName("start() sets running to true, applies autoStartup if present, and schedules reconcile task")
        void startSchedulesTaskAndAppliesAutoStartup() {
            Duration period = Duration.ofSeconds(10);
            outboxProperties.setAutoStartup(true);
            outboxProperties.setReconcilePeriod(period);

            when(outboxLifecycleController.getEndpoints()).thenReturn(List.of("partition-1"));
            doReturn(scheduledFuture)
                .when(taskScheduler)
                .scheduleAtFixedRate(any(Runnable.class), any(Instant.class), eq(period));

            reconciler.start();

            assertThat(reconciler.isRunning()).isTrue();
            assertThat(outboxMetadataStore.get("partition-1")).isEqualTo("true");

            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            verify(taskScheduler).scheduleAtFixedRate(runnableCaptor.capture(), any(Instant.class), eq(period));

            runnableCaptor.getValue().run();
            verify(outboxLifecycleController).getEndpointsRunningStatus();
        }

        @Test
        @DisplayName("start() skips setAutoStartup when outboxProperties.getAutoStartup() is null")
        void startSkipsSetAutoStartupWhenPropertyIsNull() {
            outboxProperties.setAutoStartup(null);
            outboxMetadataStore.put("partition-1", "false");
            doReturn(scheduledFuture)
                .when(taskScheduler)
                .scheduleAtFixedRate(
                    any(Runnable.class),
                    any(Instant.class),
                    eq(outboxProperties.getReconcilePeriod())
                );

            reconciler.start();

            assertThat(reconciler.isRunning()).isTrue();
            assertThat(outboxMetadataStore.get("partition-1")).isEqualTo("false");
            verify(outboxLifecycleController, never()).getEndpoints();
        }

        @Test
        @DisplayName("start() is idempotent when called multiple times")
        void startIsIdempotent() {
            doReturn(scheduledFuture)
                .when(taskScheduler)
                .scheduleAtFixedRate(
                    any(Runnable.class),
                    any(Instant.class),
                    eq(outboxProperties.getReconcilePeriod())
                );

            reconciler.start();
            reconciler.start();

            assertThat(reconciler.isRunning()).isTrue();
            verify(taskScheduler, times(1)).scheduleAtFixedRate(
                any(Runnable.class),
                any(Instant.class),
                eq(outboxProperties.getReconcilePeriod())
            );
        }

        @Test
        @DisplayName("stop() cancels scheduled task without interrupting and sets running to false")
        void stopCancelsScheduledTask() {
            doReturn(scheduledFuture)
                .when(taskScheduler)
                .scheduleAtFixedRate(
                    any(Runnable.class),
                    any(Instant.class),
                    eq(outboxProperties.getReconcilePeriod())
                );

            reconciler.start();
            assertThat(reconciler.isRunning()).isTrue();

            reconciler.stop();

            assertThat(reconciler.isRunning()).isFalse();
            verify(scheduledFuture).cancel(false);
        }

        @Test
        @DisplayName("stop() is a no-op when reconciler is not running")
        void stopIsNoOpWhenNotRunning() {
            reconciler.stop();

            assertThat(reconciler.isRunning()).isFalse();
            verifyNoInteractions(scheduledFuture);
        }
    }
}
