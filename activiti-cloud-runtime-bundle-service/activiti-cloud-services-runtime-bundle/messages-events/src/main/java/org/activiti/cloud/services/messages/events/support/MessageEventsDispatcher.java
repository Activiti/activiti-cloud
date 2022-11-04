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

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.messages.events.MessageEventHeaders;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class MessageEventsDispatcher {

    private final StreamBridge streamBridge;
    private final BindingServiceProperties bindingServiceProperties;
    private final String binding;

    public MessageEventsDispatcher(StreamBridge streamBridge,
                                   BindingServiceProperties bindingServiceProperties,
                                   String binding) {
        this.streamBridge = streamBridge;
        this.bindingServiceProperties = bindingServiceProperties;
        this.binding = binding;
    }

    public void dispatch(Message<?> message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("requires active transaction synchronization");
        }

        String messageEventOutputDestination =
                bindingServiceProperties.getBindingDestination(ProcessEngineChannels.COMMAND_PROCESSOR_INPUT_BINDING); //TODO

        Message<?> dispatchMessage = MessageBuilder.fromMessage(message)
                                                   .setHeader(MessageEventHeaders.MESSAGE_EVENT_OUTPUT_DESTINATION,
                                                              messageEventOutputDestination)
                                                   .build();

        TransactionSynchronizationManager.registerSynchronization(
                new MessageSenderTransactionSynchronization(dispatchMessage, streamBridge, binding));
    }

}
