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
package org.activiti.cloud.services.events.listeners;

import java.util.Collection;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.EventChunker;
import org.activiti.cloud.services.events.message.MessageBuilderChainFactory;
import org.activiti.cloud.services.events.services.IncidentService;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

@Transactional
public class MessageProducerCommandContextCloseListener implements CommandContextCloseListener {

    public static final String ROOT_EXECUTION_CONTEXT = "rootExecutionContext";
    public static final String PROCESS_ENGINE_EVENTS = "processEngineEvents";

    private final ProcessEngineChannels producer;
    private final MessageBuilderChainFactory<ExecutionContext> messageBuilderChainFactory;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private RuntimeBundleProperties runtimeBundleProperties;
    private final EventChunker eventChunker;
    private final IncidentService incidentService;

    public MessageProducerCommandContextCloseListener(
        ProcessEngineChannels producer,
        MessageBuilderChainFactory<ExecutionContext> messageBuilderChainFactory,
        RuntimeBundleInfoAppender runtimeBundleInfoAppender,
        RuntimeBundleProperties runtimeBundleProperties,
        EventChunker eventChunker,
        IncidentService incidentService
    ) {
        Assert.notNull(producer, "producer must not be null");
        Assert.notNull(messageBuilderChainFactory, "messageBuilderChainFactory must not be null");
        Assert.notNull(runtimeBundleInfoAppender, "runtimeBundleInfoAppender must not be null");
        Assert.notNull(eventChunker, "eventChunker must not be null");
        Assert.notNull(incidentService, "incidentService must not be null");

        this.producer = producer;
        this.messageBuilderChainFactory = messageBuilderChainFactory;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.eventChunker = eventChunker;
        this.incidentService = incidentService;
    }

    @Override
    public void closed(CommandContext commandContext) {
        List<CloudRuntimeEvent<?, ?>> events = commandContext.getGenericAttribute(PROCESS_ENGINE_EVENTS);
        if (CollectionUtils.isEmpty(events)) {
            return;
        }

        ExecutionContext rootExecutionContext = commandContext.getGenericAttribute(ROOT_EXECUTION_CONTEXT);
        sendEvents(events, rootExecutionContext);
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

    private void sendEvents(List<CloudRuntimeEvent<?, ?>> events, ExecutionContext rootExecutionContext) {
        try {
            var eventChunks = createEventChunks(events);

            eventChunks.forEach(chunk -> sendChunk(rootExecutionContext, chunk));
        } catch (IllegalArgumentException e) {
            this.incidentService.createAndSendIncidentEvent(rootExecutionContext, e);

            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private Collection<List<CloudRuntimeEventImpl<?, ?>>> createEventChunks(List<CloudRuntimeEvent<?, ?>> events) {
        var processedEvents = processEvents(events);

        if (isChunkingDisabled()) {
            return List.of(processedEvents);
        }

        return this.eventChunker.chunk(processedEvents);
    }

    private List<CloudRuntimeEventImpl<?, ?>> processEvents(List<CloudRuntimeEvent<?, ?>> events) {
        return events
            .stream()
            .filter(CloudRuntimeEventImpl.class::isInstance)
            .map(CloudRuntimeEventImpl.class::cast)
            .map(this.runtimeBundleInfoAppender::appendRuntimeBundleInfoTo)
            .toList();
    }

    private void sendChunk(ExecutionContext rootExecutionContext, List<CloudRuntimeEventImpl<?, ?>> chunk) {
        var eventArray = chunk.toArray(CloudRuntimeEvent<?, ?>[]::new);
        var message = this.messageBuilderChainFactory.create(rootExecutionContext).withPayload(eventArray).build();

        this.producer.auditProducer().send(message);
    }

    private boolean isChunkingDisabled() {
        return this.runtimeBundleProperties.getEventsProperties().isChunkingCloseListenerDisabled();
    }
}
