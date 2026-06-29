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

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

class InFlightMessageTrackerTest {

    private final InFlightMessageTracker tracker = new InFlightMessageTracker();

    private final Message<String> message = new GenericMessage<>("payload");

    @Test
    void shouldIncrementOnPreSendAndDecrementOnAfterMessageHandled() {
        tracker.preSend(message, null);
        assertThat(tracker.inFlight()).isEqualTo(1);

        tracker.afterMessageHandled(message, null, null, null);
        assertThat(tracker.inFlight()).isZero();
    }

    @Test
    void shouldDecrementOnAfterMessageHandledWhenHandlingFailed() {
        tracker.preSend(message, null);

        tracker.afterMessageHandled(message, null, null, new RuntimeException("boom"));

        assertThat(tracker.inFlight()).isZero();
    }

    @Test
    void shouldDecrementOnAfterSendCompletionWhenMessageWasNotSent() {
        tracker.preSend(message, null);

        tracker.afterSendCompletion(message, null, false, null);

        assertThat(tracker.inFlight()).isZero();
    }

    @Test
    void shouldDecrementOnAfterSendCompletionWhenSendThrew() {
        tracker.preSend(message, null);

        tracker.afterSendCompletion(message, null, false, new RuntimeException("boom"));

        assertThat(tracker.inFlight()).isZero();
    }

    @Test
    void shouldNotDecrementOnAfterSendCompletionWhenMessageWasSentSuccessfully() {
        tracker.preSend(message, null);

        tracker.afterSendCompletion(message, null, true, null);

        assertThat(tracker.inFlight()).isEqualTo(1);
    }

    @Test
    void shouldCountMultipleConcurrentRequests() {
        tracker.preSend(message, null);
        tracker.preSend(message, null);
        tracker.preSend(message, null);
        assertThat(tracker.inFlight()).isEqualTo(3);

        tracker.afterMessageHandled(message, null, null, null);
        assertThat(tracker.inFlight()).isEqualTo(2);
    }
}
