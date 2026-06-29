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
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.binding.InputBindingLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;

@ExtendWith(MockitoExtension.class)
class PartitionedChannelGracefulShutdownTest {

    @Mock
    private InputBindingLifecycle inputBindingLifecycle;

    private PartitionedChannelGracefulShutdown gracefulShutdown;

    @BeforeEach
    void setUp() {
        gracefulShutdown = new PartitionedChannelGracefulShutdown(inputBindingLifecycle, Duration.ofSeconds(5));
        gracefulShutdown.start();
    }

    @Test
    void shouldExposeChannelInterceptorThatTracksInFlight() {
        final Message<String> message = new GenericMessage<>("payload");
        final ExecutorChannelInterceptor interceptor =
            (ExecutorChannelInterceptor) gracefulShutdown.channelInterceptor();

        interceptor.preSend(message, null);
        assertThat(gracefulShutdown.inFlight()).isEqualTo(1);

        interceptor.afterMessageHandled(message, null, null, null);
        assertThat(gracefulShutdown.inFlight()).isZero();
    }

    @Test
    void shouldStopConsumptionAndCompleteWhenNothingInFlight() {
        gracefulShutdown.stop();

        verify(inputBindingLifecycle).stop();
        assertThat(gracefulShutdown.isRunning()).isFalse();
        assertThat(gracefulShutdown.inFlight()).isZero();
    }

    @Test
    void shouldHaveMaxPhaseSoItStopsFirst() {
        assertThat(gracefulShutdown.getPhase()).isEqualTo(Integer.MAX_VALUE);
    }
}
