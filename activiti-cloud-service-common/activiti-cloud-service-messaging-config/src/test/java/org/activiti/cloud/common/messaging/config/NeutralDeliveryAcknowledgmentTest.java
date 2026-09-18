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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class NeutralDeliveryAcknowledgmentTest {

    private final NeutralDeliveryAcknowledgment acknowledgment = new NeutralDeliveryAcknowledgment();

    @Mock
    private AcknowledgmentCallback acknowledgmentCallback;

    @Test
    void should_acceptCallback_when_acknowledging() {
        var message = MessageBuilder.withPayload("payload")
            .setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, acknowledgmentCallback)
            .build();

        acknowledgment.acknowledge(message);

        verify(acknowledgmentCallback).acknowledge(AcknowledgmentCallback.Status.ACCEPT);
    }

    @Test
    void should_requeueCallback_when_requeueing() {
        var message = MessageBuilder.withPayload("payload")
            .setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, acknowledgmentCallback)
            .build();

        acknowledgment.requeue(message);

        verify(acknowledgmentCallback).acknowledge(AcknowledgmentCallback.Status.REQUEUE);
    }

    @Test
    void should_beNoOp_when_noAcknowledgmentCallback() {
        var message = MessageBuilder.withPayload("payload").build();

        assertThatCode(() -> {
            acknowledgment.acknowledge(message);
            acknowledgment.requeue(message);
        }).doesNotThrowAnyException();

        verifyNoInteractions(acknowledgmentCallback);
    }

    @Test
    void should_stripOnlyTheNeutralCallbackHeader() {
        assertThat(acknowledgment.acknowledgmentHeaders())
            .containsExactly(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK)
            .doesNotContain(AmqpHeaders.CHANNEL, AmqpHeaders.DELIVERY_TAG);
    }
}
