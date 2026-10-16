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

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.core.NestedExceptionUtils;

public class RequeueRepublishMessageRecoverer implements MessageRecoverer {

    private static final int MAX_RETRIES = 3;
    private static final Logger log = LoggerFactory.getLogger(RequeueRepublishMessageRecoverer.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessageRecoverer republishMessageRecoverer;

    private int maxRetries = MAX_RETRIES;
    private String dlxName;

    public RequeueRepublishMessageRecoverer(RabbitTemplate rabbitTemplate, MessageRecoverer republishMessageRecoverer) {
        this.rabbitTemplate = rabbitTemplate;
        this.republishMessageRecoverer = republishMessageRecoverer;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        // Track current retries
        final long currentRetry = message.getMessageProperties().getRetryCount();

        if (currentRetry < getMaxRetries()) {
            message.getMessageProperties().incrementRetryCount();
            message.getMessageProperties().setRedelivered(true);

            // Identify original destination coordinates
            final var exchange = Optional.ofNullable(this.dlxName).orElseGet(() ->
                message.getMessageProperties().getReceivedExchange()
            );
            final var routingKey = Optional.ofNullable(this.dlxName)
                .map(it -> message.getMessageProperties().getConsumerQueue())
                .orElseGet(() -> message.getMessageProperties().getReceivedRoutingKey());

            log.warn("Retry {}/{} for message {}", message.getMessageProperties().getRetryCount(), maxRetries, message);

            // Send directly back to the queue via the default exchange using the queue name as the routing key
            rabbitTemplate.send(exchange, routingKey, message);
        } else {
            log.warn("Exceeded max {} retries. Republishing message {} to parking lot", MAX_RETRIES, message);
            republishMessageRecoverer.recover(message, NestedExceptionUtils.getMostSpecificCause(cause));
        }
    }

    public RequeueRepublishMessageRecoverer maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;

        return this;
    }

    public RequeueRepublishMessageRecoverer dlxName(String dlxName) {
        this.dlxName = dlxName;
        return this;
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}
