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

import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.store.MessageGroup;

public class MessageGroupProcessorHandlerChain implements MessageGroupProcessor {
    
    private final MessageGroupProcessorChain chain;
    
    public MessageGroupProcessorHandlerChain(MessageGroupProcessorChain chain) {
        this.chain = chain;
    }

    @Override
    public Object processMessageGroup(MessageGroup group) {
        return chain.handle(group);
    }
    
}
