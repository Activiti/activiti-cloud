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

package org.activiti.cloud.services.messages.core.processor;

import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.MESSAGE_PAYLOAD_TYPE;
import static org.activiti.cloud.services.messages.core.support.Predicates.MESSAGE_SENT;
import static org.activiti.cloud.services.messages.core.support.Predicates.MESSAGE_WAITING;

import java.util.Collection;

import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.cloud.services.messages.core.support.MessageComparators;
import org.activiti.cloud.services.messages.core.transformer.ReceiveMessagePayloadTransformer;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

public class ReceiveMessagePayloadGroupProcessor extends AbstractMessageGroupProcessorHandler {
    
    private final MessageGroupStore messageGroupStore;

    public ReceiveMessagePayloadGroupProcessor(MessageGroupStore messageGroupStore) {
        this.messageGroupStore = messageGroupStore;
    }
    
    @Override
    protected Message<?> process(MessageGroup group) {
        Message<?> result = group.getMessages()
                                 .stream()
                                 .filter(MESSAGE_SENT)
                                 .min(MessageComparators.TIMESTAMP)
                                 .get();

        messageGroupStore.removeMessagesFromGroup(group.getGroupId(),
                                                  result);
        return buildOutputMessage(result);     
    }
    
    
    protected Message<?> buildOutputMessage(Message<?> message) {
        ReceiveMessagePayload payload = ReceiveMessagePayloadTransformer.from(message);
        
        return MessageBuilder.withPayload(payload)
                             .setHeader(MESSAGE_PAYLOAD_TYPE,
                                        ReceiveMessagePayload.class.getSimpleName())
                             .build();
    }
    
    @Override
    protected boolean canProcess(MessageGroup group) {
        Collection<Message<?>> messages = group.getMessages();
             
        return messages.stream().anyMatch(MESSAGE_WAITING) 
                && messages.stream().anyMatch(MESSAGE_SENT);
    }
}
