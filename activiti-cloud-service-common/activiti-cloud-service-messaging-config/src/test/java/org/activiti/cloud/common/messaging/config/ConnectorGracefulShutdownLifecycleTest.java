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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.binding.InputBindingLifecycle;

@ExtendWith(MockitoExtension.class)
class ConnectorGracefulShutdownLifecycleTest {

    @Mock
    private InFlightMessageTracker inFlightTracker;

    @Mock
    private InputBindingLifecycle inputBindingLifecycle;

    private ConnectorGracefulShutdownLifecycle gracefulShutdown;

    @BeforeEach
    void setUp() {
        gracefulShutdown = new ConnectorGracefulShutdownLifecycle(
            inFlightTracker,
            inputBindingLifecycle,
            Duration.ofSeconds(5)
        );
        gracefulShutdown.start();
    }

    @Test
    void shouldBeRunningAfterStart() {
        assertThat(gracefulShutdown.isRunning()).isTrue();
    }

    @Test
    void shouldStopConsumptionWhenShutdownStarts() {
        when(inFlightTracker.inFlight()).thenReturn(0);

        gracefulShutdown.stop();

        verify(inputBindingLifecycle).stop();
        assertThat(gracefulShutdown.isRunning()).isFalse();
    }

    @Test
    void shouldWaitForInFlightRequestsToFinishBeforeCompleting() {
        when(inFlightTracker.inFlight()).thenReturn(2, 1, 0);

        gracefulShutdown.stop();

        verify(inputBindingLifecycle).stop();
        verify(inFlightTracker, times(3)).inFlight();
    }

    @Test
    void shouldForceShutdownWhenTimeoutExceeded() {
        gracefulShutdown = new ConnectorGracefulShutdownLifecycle(
            inFlightTracker,
            inputBindingLifecycle,
            Duration.ofMillis(200)
        );
        gracefulShutdown.start();
        when(inFlightTracker.inFlight()).thenReturn(5);

        final long start = System.nanoTime();
        gracefulShutdown.stop();
        final Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        verify(inputBindingLifecycle).stop();
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
        assertThat(gracefulShutdown.isRunning()).isFalse();
    }

    @Test
    void shouldDoNothingWhenAlreadyStopped() {
        gracefulShutdown.stop();

        gracefulShutdown.stop();

        verify(inputBindingLifecycle, times(1)).stop();
    }
}
