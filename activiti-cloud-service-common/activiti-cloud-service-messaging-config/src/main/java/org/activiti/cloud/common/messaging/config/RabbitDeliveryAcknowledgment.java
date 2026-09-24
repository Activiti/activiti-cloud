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

import com.rabbitmq.client.Channel;
import java.util.Collection;
import java.util.List;
import java.util.function.ObjLongConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.messaging.Message;

/**
 * {@link DeliveryAcknowledgment} for the classic RabbitMQ Spring Cloud Stream binder, which does not
 * populate Spring Integration's {@link AcknowledgmentCallback} in manual-ack mode - it exposes only
 * the raw AMQP channel and delivery tag. Prefers the neutral callback when it is present (so it is
 * also correct for a message delivered by another binder), and falls back to
 * {@code channel.basicAck}/{@code basicNack} otherwise.
 */
class RabbitDeliveryAcknowledgment implements DeliveryAcknowledgment {

    private static final Logger log = LoggerFactory.getLogger(RabbitDeliveryAcknowledgment.class);

    @Override
    public void acknowledge(Message<?> message) {
        final var callback = StaticMessageHeaderAccessor.getAcknowledgmentCallback(message);
        if (callback != null) {
            callback.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
            return;
        }

        withChannelAndDeliveryTag(message, (channel, deliveryTag) -> {
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                log.warn("Failed to acknowledge message {}", message, e);
            }
        });
    }

    @Override
    public void requeue(Message<?> message) {
        final var callback = StaticMessageHeaderAccessor.getAcknowledgmentCallback(message);
        if (callback != null) {
            callback.acknowledge(AcknowledgmentCallback.Status.REQUEUE);
            return;
        }

        withChannelAndDeliveryTag(message, (channel, deliveryTag) -> {
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception e) {
                log.warn("Failed to negatively acknowledge message {}", message, e);
            }
        });
    }

    @Override
    public Collection<String> acknowledgmentHeaders() {
        return List.of(
            AmqpHeaders.CHANNEL,
            AmqpHeaders.DELIVERY_TAG,
            IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK
        );
    }

    private static void withChannelAndDeliveryTag(Message<?> message, ObjLongConsumer<Channel> action) {
        var channel = message.getHeaders().get(AmqpHeaders.CHANNEL, Channel.class);
        var deliveryTag = message.getHeaders().get(AmqpHeaders.DELIVERY_TAG, Long.class);

        if (channel != null && deliveryTag != null) {
            action.accept(channel, deliveryTag);
        }
    }
}
