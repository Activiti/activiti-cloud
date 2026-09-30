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

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;

class PushedCountsQueryEventsConsumerTest {

    private final RecomputeEventCapturer recomputeEventCapturer = mock(RecomputeEventCapturer.class);
    private final PushedCountsQueryEventsConsumer consumer = new PushedCountsQueryEventsConsumer(
        recomputeEventCapturer
    );

    @Test
    void feedsTheMessagePayloadToTheCapturer() {
        List<CloudRuntimeEvent<?, ?>> events = List.of();

        consumer.accept(MessageBuilder.withPayload(events).build());

        verify(recomputeEventCapturer).capture(events);
    }

    @Test
    void aFailingCapture_isLoggedAndSkipped_ratherThanPropagated() {
        List<CloudRuntimeEvent<?, ?>> events = List.of();
        doThrow(new RuntimeException("boom")).when(recomputeEventCapturer).capture(events);

        assertThatNoException().isThrownBy(() -> consumer.accept(MessageBuilder.withPayload(events).build()));
    }
}
