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

package org.activiti.cloud.services.messages.core.aggregator;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Message Connector Aggregator specific implementation of {@link AbstractCorrelatingMessageHandler}.
 * Will remove {@link MessageGroup}s in the {@linkplain #afterRelease}
 * only if 'expireGroupsUponCompletion' flag is set to 'true'.
 *
 */
public class MessageConnectorAggregator extends AbstractCorrelatingMessageHandler {

    private volatile boolean expireGroupsUponCompletion = false;
    private volatile boolean completeGroupsWhenEmpty = false;

    public MessageConnectorAggregator(MessageGroupProcessor processor, MessageGroupStore store,
            CorrelationStrategy correlationStrategy, ReleaseStrategy releaseStrategy) {
        super(processor, store, correlationStrategy, releaseStrategy);
    }

    public MessageConnectorAggregator(MessageGroupProcessor processor, MessageGroupStore store) {
        super(processor, store);
    }

    public MessageConnectorAggregator(MessageGroupProcessor processor) {
        super(processor);
    }

    /**
     * Will set the 'expireGroupsUponCompletion' flag.
     *
     * @param expireGroupsUponCompletion true when groups should be expired on completion.
     *
     * @see #afterRelease
     */
    public void setExpireGroupsUponCompletion(boolean expireGroupsUponCompletion) {
        this.expireGroupsUponCompletion = expireGroupsUponCompletion;
    }

    @Override
    protected boolean isExpireGroupsUponCompletion() {
        return this.expireGroupsUponCompletion;
    }

    
    public boolean isCompleteGroupsWhenEmpty() {
        return completeGroupsWhenEmpty;
    }

    
    public void setCompleteGroupsWhenEmpty(boolean completeGroupsWhenEmpty) {
        this.completeGroupsWhenEmpty = completeGroupsWhenEmpty;
    }    
    /**
     * Remove all completed messages from group. Complete the group if empty and remove if expired 
     * If the {@link #expireGroupsUponCompletion} is true, then remove group fully.
     * @param messageGroup the group to clean up.
     * @param completedMessages The completed messages. 
     */
    @Override
    protected void afterRelease(MessageGroup messageGroup, 
                                @Nullable Collection<Message<?>> completedMessages) {
        Object groupId = messageGroup.getGroupId();
        MessageGroupStore messageStore = getMessageStore();
        boolean isCompleted = false;

        if (completedMessages != null && !completedMessages.isEmpty()) {
            Collection<Message<?>> deletedMessages = messageGroup.getMessages()
                        .stream()
                        .filter(completedMessages::contains)
                        .collect(Collectors.toList());
                        
            if (!deletedMessages.isEmpty()) {
                messageStore.removeMessagesFromGroup(groupId, deletedMessages);
            }
        }
        
        if (this.completeGroupsWhenEmpty) {
            if (messageStore.messageGroupSize(groupId) == 0) {
                messageStore.completeGroup(groupId);
                isCompleted = true;
            }
        }
        
        if (this.expireGroupsUponCompletion && isCompleted) {
            remove(messageGroup);
        } 
    }
    
}
