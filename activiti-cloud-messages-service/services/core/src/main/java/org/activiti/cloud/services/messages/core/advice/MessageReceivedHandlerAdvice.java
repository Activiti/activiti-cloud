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

package org.activiti.cloud.services.messages.core.advice;

import static org.activiti.cloud.services.messages.core.support.MessageComparators.TIMESTAMP;
import static org.activiti.cloud.services.messages.core.support.Predicates.MESSAGE_RECEIVED;
import static org.activiti.cloud.services.messages.core.support.Predicates.MESSAGE_WAITING;

import org.activiti.cloud.services.messages.core.support.LockTemplate;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.messaging.Message;

public class MessageReceivedHandlerAdvice extends AbstractMessageConnectorHandlerAdvice {

    private final MessageGroupStore messageStore;
    private final LockTemplate lockTemplate;
    private final CorrelationStrategy correlationStrategy;
    
    public MessageReceivedHandlerAdvice(MessageGroupStore messageStore,
                                        CorrelationStrategy correlationStrategy,
                                        LockTemplate lockTemplate) {
        this.messageStore = messageStore;
        this.lockTemplate = lockTemplate;
        this.correlationStrategy = correlationStrategy;
    }
    
    @Override
    public <T> T doHandle(Message<?> message) {
        Object groupId = correlationStrategy.getCorrelationKey(message);
        Object key = UUIDConverter.getUUID(groupId).toString();

        lockTemplate.lockInterruptibly(key, () -> {
            MessageGroup group = messageStore.getMessageGroup(groupId);
            
            group.getMessages()
                 .stream()
                 .filter(MESSAGE_WAITING)
                 .min(TIMESTAMP)
                 .ifPresent(result -> {
                     messageStore.removeMessagesFromGroup(groupId, result);                            
                 });
        });
        
        return null;
        
    }

    @Override
    public boolean canHandle(Message<?> message) {
        return MESSAGE_RECEIVED.test(message);
    }
    
}
