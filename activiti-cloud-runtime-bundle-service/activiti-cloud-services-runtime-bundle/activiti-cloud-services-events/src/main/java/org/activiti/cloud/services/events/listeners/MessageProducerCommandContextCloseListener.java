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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.EventChunker;
import org.activiti.cloud.services.events.message.MessageBuilderChainFactory;
import org.activiti.cloud.services.events.services.IncidentService;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

@Transactional
public class MessageProducerCommandContextCloseListener implements CommandContextCloseListener {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerCommandContextCloseListener.class);

    public static final String ROOT_EXECUTION_CONTEXT = "rootExecutionContext";
    public static final String PROCESS_ENGINE_EVENTS = "processEngineEvents";

    /**
     * Feature flag (canonical key {@code activiti.features.split-process-created-event.enabled}).
     * When enabled, the {@code PROCESS_CREATED} event for the root process instance is published as a
     * standalone message ahead of the rest of the events accumulated in the same command context, so
     * the consumer can materialise the process row immediately and the user sees it without waiting
     * for the bulk of TASK/VARIABLE/ACTIVITY events to be handled.
     */
    public static final String SPLIT_PROCESS_CREATED_EVENT_FEATURE = "split-process-created-event";

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
            List<CloudRuntimeEvent<?, ?>> remaining = events;

            if (FeatureToggleHolder.isEnabled(SPLIT_PROCESS_CREATED_EVENT_FEATURE)) {
                int rootCreatedIndex = findRootProcessCreatedEventIndex(events, rootExecutionContext);
                if (rootCreatedIndex >= 0) {
                    String rootProcessInstanceId = rootExecutionContext.getProcessInstance().getId();
                    logger.info(
                        "Feature '{}' enabled: publishing root PROCESS_CREATED for process instance '{}' as a standalone message ahead of the remaining {} event(s)",
                        SPLIT_PROCESS_CREATED_EVENT_FEATURE,
                        rootProcessInstanceId,
                        events.size() - 1
                    );
                    sendChunk(rootExecutionContext, processEvents(List.of(events.get(rootCreatedIndex))));
                    remaining = withoutIndex(events, rootCreatedIndex);
                }
            }

            if (remaining.isEmpty()) {
                return;
            }

            var eventChunks = createEventChunks(remaining);

            eventChunks.forEach(chunk -> sendChunk(rootExecutionContext, chunk));
        } catch (IllegalArgumentException e) {
            this.incidentService.createAndSendIncidentEvent(rootExecutionContext, e);

            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private int findRootProcessCreatedEventIndex(
        List<CloudRuntimeEvent<?, ?>> events,
        ExecutionContext rootExecutionContext
    ) {
        if (rootExecutionContext == null || rootExecutionContext.getProcessInstance() == null) {
            return -1;
        }
        String rootProcessInstanceId = rootExecutionContext.getProcessInstance().getId();
        for (int i = 0; i < events.size(); i++) {
            CloudRuntimeEvent<?, ?> event = events.get(i);
            if (
                event instanceof CloudProcessCreatedEvent &&
                rootProcessInstanceId.equals(event.getEntityId())
            ) {
                return i;
            }
        }
        return -1;
    }

    private static List<CloudRuntimeEvent<?, ?>> withoutIndex(List<CloudRuntimeEvent<?, ?>> events, int indexToSkip) {
        List<CloudRuntimeEvent<?, ?>> result = new ArrayList<>(events.size() - 1);
        for (int i = 0; i < events.size(); i++) {
            if (i != indexToSkip) {
                result.add(events.get(i));
            }
        }
        return result;
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
