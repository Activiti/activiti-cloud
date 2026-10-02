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
package org.activiti.cloud.common.messaging.function.router;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.core.NestedExceptionUtils;

public class RequeueMessageRecoverer implements MessageRecoverer {

    private static final String X_RETRY_COUNT = "x-retry-count";
    private static final int MAX_RETRIES = 3;

    private final RabbitTemplate rabbitTemplate;

    private final RepublishMessageRecoverer republishMessageRecoverer;

    public RequeueMessageRecoverer(RabbitTemplate rabbitTemplate, RepublishMessageRecoverer republishMessageRecoverer) {
        this.rabbitTemplate = rabbitTemplate;
        this.republishMessageRecoverer = republishMessageRecoverer;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        // Track current retries
        final long currentRetry = message.getMessageProperties().getRetryCount();

        if (currentRetry < MAX_RETRIES) {
            message.getMessageProperties().incrementRetryCount();
            message.getMessageProperties().setRedelivered(true);

            // Identify original destination coordinates
            final String originalExchange = message.getMessageProperties().getReceivedExchange();
            final String originalRoutingKey = message.getMessageProperties().getReceivedRoutingKey();

            // Send directly back to the queue via the default exchange using the queue name as the routing key
            rabbitTemplate.send(originalExchange, originalRoutingKey, message);
        } else {
            // Exceeded max retries: fall back to the standard DLQ routing behavior
            republishMessageRecoverer.recover(message, NestedExceptionUtils.getMostSpecificCause(cause));
        }
    }
}
