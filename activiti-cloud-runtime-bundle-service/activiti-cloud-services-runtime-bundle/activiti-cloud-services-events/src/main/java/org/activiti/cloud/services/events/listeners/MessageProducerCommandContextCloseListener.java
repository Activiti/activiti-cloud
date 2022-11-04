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
package org.activiti.cloud.services.events.listeners;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.MessageBuilderChainFactory;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

import static org.activiti.cloud.services.events.ProcessEngineChannels.AUDIT_PRODUCER_OUTPUT_BINDING;

@Transactional
public class MessageProducerCommandContextCloseListener implements CommandContextCloseListener {

    public static final String ROOT_EXECUTION_CONTEXT = "rootExecutionContext";

    public static final String PROCESS_ENGINE_EVENTS = "processEngineEvents";

    private final StreamBridge streamBridge;

    private final MessageBuilderChainFactory<ExecutionContext> messageBuilderChainFactory;

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public MessageProducerCommandContextCloseListener(MessageBuilderChainFactory<ExecutionContext> messageBuilderChainFactory,
                                                      RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                                      StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
        Assert.notNull(messageBuilderChainFactory,
                       "messageBuilderChainFactory must not be null");
        Assert.notNull(runtimeBundleInfoAppender,
                "runtimeBundleInfoAppender must not be null");

        this.messageBuilderChainFactory = messageBuilderChainFactory;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    @Override
    public void closed(CommandContext commandContext) {
        List<CloudRuntimeEvent<?, ?>> events = commandContext.getGenericAttribute(PROCESS_ENGINE_EVENTS);

        if (events != null && !events.isEmpty()) {
            ExecutionContext rootExecutionContext = commandContext.getGenericAttribute(ROOT_EXECUTION_CONTEXT);

            // Add runtime bundle context attributes to every event
            CloudRuntimeEvent<?, ?>[] payload = events.stream()
                                                      .filter(CloudRuntimeEventImpl.class::isInstance)
                                                      .map(CloudRuntimeEventImpl.class::cast)
                                                      .map(runtimeBundleInfoAppender::appendRuntimeBundleInfoTo)
                                                      .toArray(CloudRuntimeEvent<?, ?>[]::new);

            // Inject message headers with null execution context as there may be events from several process instances
            Message<CloudRuntimeEvent<?, ?>[]> message = messageBuilderChainFactory.create(rootExecutionContext)
                                                                                   .withPayload(payload)
                                                                                   .build();
            // Send message to audit producer channel
            streamBridge.send(AUDIT_PRODUCER_OUTPUT_BINDING, message);
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
