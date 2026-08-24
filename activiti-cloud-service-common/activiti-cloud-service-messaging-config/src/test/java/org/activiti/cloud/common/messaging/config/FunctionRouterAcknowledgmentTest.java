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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class FunctionRouterAcknowledgmentTest {

    private static final long DELIVERY_TAG = 42L;

    @Mock
    private AcknowledgmentCallback acknowledgmentCallback;

    @Mock
    private Channel channel;

    @Test
    void should_useNeutralCallbackAccept_when_acknowledgmentCallbackPresent() {
        var message = MessageBuilder.withPayload("payload")
            .setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, acknowledgmentCallback)
            .setHeader(AmqpHeaders.CHANNEL, channel)
            .setHeader(AmqpHeaders.DELIVERY_TAG, DELIVERY_TAG)
            .build();

        FunctionRouterConfiguration.acknowledge(message);

        verify(acknowledgmentCallback).acknowledge(AcknowledgmentCallback.Status.ACCEPT);
        verifyNoInteractions(channel);
    }

    @Test
    void should_useNeutralCallbackRequeue_when_acknowledgmentCallbackPresent() {
        var message = MessageBuilder.withPayload("payload")
            .setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, acknowledgmentCallback)
            .setHeader(AmqpHeaders.CHANNEL, channel)
            .setHeader(AmqpHeaders.DELIVERY_TAG, DELIVERY_TAG)
            .build();

        FunctionRouterConfiguration.negativelyAcknowledgeAndRequeue(message);

        verify(acknowledgmentCallback).acknowledge(AcknowledgmentCallback.Status.REQUEUE);
        verifyNoInteractions(channel);
    }

    @Test
    void should_fallBackToChannelBasicAck_when_noAcknowledgmentCallback() throws Exception {
        var message = MessageBuilder.withPayload("payload")
            .setHeader(AmqpHeaders.CHANNEL, channel)
            .setHeader(AmqpHeaders.DELIVERY_TAG, DELIVERY_TAG)
            .build();

        FunctionRouterConfiguration.acknowledge(message);

        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void should_fallBackToChannelBasicNackWithRequeue_when_noAcknowledgmentCallback() throws Exception {
        var message = MessageBuilder.withPayload("payload")
            .setHeader(AmqpHeaders.CHANNEL, channel)
            .setHeader(AmqpHeaders.DELIVERY_TAG, DELIVERY_TAG)
            .build();

        FunctionRouterConfiguration.negativelyAcknowledgeAndRequeue(message);

        verify(channel).basicNack(DELIVERY_TAG, false, true);
    }

    @Test
    void should_beNoOp_when_noAcknowledgmentHandlePresent() {
        var message = MessageBuilder.withPayload("payload").build();

        assertThatCode(() -> {
            FunctionRouterConfiguration.acknowledge(message);
            FunctionRouterConfiguration.negativelyAcknowledgeAndRequeue(message);
        }).doesNotThrowAnyException();

        verifyNoInteractions(channel, acknowledgmentCallback);
    }
}
