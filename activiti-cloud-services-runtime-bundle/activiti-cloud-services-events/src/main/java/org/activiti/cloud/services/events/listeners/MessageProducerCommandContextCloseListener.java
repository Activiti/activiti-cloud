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

package org.activiti.cloud.services.events.listeners;

import java.util.List;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.ExecutionContextInfoAppender;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.MessageBuilderChainFactory;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

public class MessageProducerCommandContextCloseListener implements CommandContextCloseListener {

    public static final String PROCESS_ENGINE_EVENTS = "processEngineEvents";
    public static final String EXECUTION_CONTEXT = "executionContext";

    private final ProcessEngineChannels producer;
    private final MessageBuilderChainFactory<ExecutionContext> messageBuilderChainFactory;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    
    public MessageProducerCommandContextCloseListener(ProcessEngineChannels producer,
            MessageBuilderChainFactory<ExecutionContext> messageBuilderChainFactory,
            RuntimeBundleInfoAppender runtimeBundleInfoAppender ) {
        Assert.notNull(producer,
                       "producer must not be null");
        Assert.notNull(messageBuilderChainFactory,
                       "messageBuilderChainFactory must not be null");
        Assert.notNull(runtimeBundleInfoAppender,
                "runtimeBundleInfoAppender must not be null");

        this.producer = producer;
        this.messageBuilderChainFactory = messageBuilderChainFactory;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }
    
    protected ExecutionContextInfoAppender createExecutionContextInfoAppender(ExecutionContext executionContext) {
        return new ExecutionContextInfoAppender(executionContext);
    }
    
    @Override
    public void closed(CommandContext commandContext) {
        List<CloudRuntimeEvent<?, ?>> events = commandContext.getGenericAttribute(PROCESS_ENGINE_EVENTS);
        
        if (events != null && !events.isEmpty()) {
            ExecutionContext executionContext = commandContext.getGenericAttribute(EXECUTION_CONTEXT);
            
            ExecutionContextInfoAppender executionContextInfoAppender = createExecutionContextInfoAppender(executionContext);

            // Add execution context attributes to every event 
            CloudRuntimeEvent<?, ?>[] payload = events.stream()
                                                      .filter(CloudRuntimeEventImpl.class::isInstance)
                                                      .map(CloudRuntimeEventImpl.class::cast)
                                                      .map(runtimeBundleInfoAppender::appendRuntimeBundleInfoTo)
                                                      .map(executionContextInfoAppender::appendExecutionContextInfoTo)
                                                      .toArray(CloudRuntimeEvent<?, ?>[]::new);

            // Inject message headers from  execution context
            Message<CloudRuntimeEvent<?, ?>[]> message = messageBuilderChainFactory.create(executionContext)
                                                                                   .withPayload(payload)
                                                                                   .build();
            // Send message to audit producer channel
            producer.auditProducer().send(message);
        }
    }

    @Override
    public void closing(CommandContext commandContext) {
        // No need to implement this method in this class
    }

    @Override
    public void afterSessionsFlush(CommandContext commandContext) {
        // No need to implement this method in this class
    }

    @Override
    public void closeFailure(CommandContext commandContext) {
        // No need to implement this method in this class
    }
}
