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

import java.util.Optional;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

public class FunctionRouterConsumerErrorRecoverer implements MessageHandler {

    private final MessageRecoverer messageRecoverer;

    public FunctionRouterConsumerErrorRecoverer(MessageRecoverer messageRecoverer) {
        this.messageRecoverer = messageRecoverer;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        if (message instanceof ErrorMessage errorMessage) {
            Optional.ofNullable(errorMessage.getOriginalMessage())
                .map(StaticMessageHeaderAccessor::getSourceData)
                .filter(org.springframework.amqp.core.Message.class::isInstance)
                .map(org.springframework.amqp.core.Message.class::cast)
                .ifPresent(amqpMessage -> messageRecoverer.recover(amqpMessage, errorMessage.getPayload()));
        }
    }
}
