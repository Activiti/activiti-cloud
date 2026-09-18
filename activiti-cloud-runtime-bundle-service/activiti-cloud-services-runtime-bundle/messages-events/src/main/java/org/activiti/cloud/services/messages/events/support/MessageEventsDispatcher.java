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
package org.activiti.cloud.services.messages.events.support;

import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.messages.events.MessageEventHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class MessageEventsDispatcher {

    private final MessageChannel messageEvents;
    private final FunctionBindingConfiguration.BindingResolver bindingResolver;

    public MessageEventsDispatcher(
        MessageChannel messageEvents,
        FunctionBindingConfiguration.BindingResolver bindingResolver
    ) {
        this.messageEvents = messageEvents;
        this.bindingResolver = bindingResolver;
    }

    public void dispatch(Message<?> message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("requires active transaction synchronization");
        }

        String messageEventOutputDestination = bindingResolver.getBindingDestination(
            ProcessEngineChannels.COMMAND_CONSUMER
        );

        Message<?> dispatchMessage = MessageBuilder.fromMessage(message)
            .setHeader(MessageEventHeaders.MESSAGE_EVENT_OUTPUT_DESTINATION, messageEventOutputDestination)
            .build();

        TransactionSynchronizationManager.registerSynchronization(
            new MessageSenderTransactionSynchronization(dispatchMessage, messageEvents)
        );
    }
}
