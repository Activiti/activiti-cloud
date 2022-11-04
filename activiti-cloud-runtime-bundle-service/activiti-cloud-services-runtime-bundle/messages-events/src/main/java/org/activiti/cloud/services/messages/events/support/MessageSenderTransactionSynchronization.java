/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.messages.events.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.messaging.Message;
import org.springframework.transaction.support.TransactionSynchronization;

public class MessageSenderTransactionSynchronization implements TransactionSynchronization {

    private static final Logger logger = LoggerFactory.getLogger(MessageSenderTransactionSynchronization.class);

    private final Message<?> message;
    private final StreamBridge streamBridge;
    private final String binding;

    public MessageSenderTransactionSynchronization(Message<?> message,
                                                   StreamBridge streamBridge,
                                                   String binding) {
        this.message = message;
        this.streamBridge = streamBridge;
        this.binding = binding;
    }

    @Override
    public void afterCommit() {
        logger.debug("Sending bpmn message '{}' via message channel: {}", message, streamBridge);

        try {
            boolean sent = streamBridge.send(binding, message);

            if(!sent) {
                throw new MessageDispatchingException(message);
            }

        } catch(Exception cause) {
            logger.error("Sending bpmn message {} failed due to error: {}", message, cause.getMessage());
        }
    }
}
