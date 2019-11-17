/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.message.events;

import org.activiti.api.process.model.events.BPMNMessageSentEvent;
import org.activiti.api.process.runtime.events.listener.BPMNElementEventListener;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageSentEvent;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class BpmnMessageSentEventMessageProducer implements BPMNElementEventListener<BPMNMessageSentEvent>  {
    private static final Logger logger = LoggerFactory.getLogger(BpmnMessageSentEventMessageProducer.class);

    private final BpmnMessageEventMessageBuilderFactory messageBuilderFactory;
    private final MessageChannel messageChannel;
    private final ToCloudProcessRuntimeEventConverter runtimeEventConverter;

    public BpmnMessageSentEventMessageProducer(@NonNull MessageChannel messageChannel,
                                               @NonNull BpmnMessageEventMessageBuilderFactory messageBuilderFactory,
                                               @NonNull ToCloudProcessRuntimeEventConverter runtimeEventConverter) {
        this.messageChannel = messageChannel;
        this.messageBuilderFactory = messageBuilderFactory;
        this.runtimeEventConverter = runtimeEventConverter;
    }
    
    @Override
    public void onEvent(@NonNull BPMNMessageSentEvent event) {
        logger.debug("onEvent: {}", event);
        
        if(!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("requires active transaction synchronization");
        }

        Message<CloudBPMNMessageSentEvent> message = messageBuilderFactory.create(event.getEntity())
                                                                          .withPayload(runtimeEventConverter.from(event))
                                                                          .build();

        TransactionSynchronizationManager.registerSynchronization(new MessageSenderTransactionSynchronization(message,
                                                                                                              messageChannel));
    }    
    
}
