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

import org.activiti.api.process.model.events.ProcessDeployedEvent;
import org.activiti.api.runtime.event.impl.ProcessDeployedEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.activiti.cloud.services.events.ProcessEngineChannels.AUDIT_PRODUCER_OUTPUT_BINDING;

public class CloudProcessDeployedProducer {

    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    private StreamBridge streamBridge;

    private RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory;

    private int chunkSize;

    public CloudProcessDeployedProducer(RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                        StreamBridge streamBridge,
                                        RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory,
                                        RuntimeBundleProperties properties) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.streamBridge = streamBridge;
        this.runtimeBundleMessageBuilderFactory = runtimeBundleMessageBuilderFactory;
        this.chunkSize = properties.getEventsProperties()
                                   .getChunkSize();
    }

    @EventListener
    public void sendProcessDeployedEvents(ProcessDeployedEvents processDeployedEvents) {
        final AtomicInteger counter = new AtomicInteger();

        processDeployedEvents.getProcessDeployedEvents()
                             .stream()
                             .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                             .values()
                             .stream()
                             .map(this::toCloudProcessDeployedEvents)
                             .forEach(this::sendCloudProcessDeployedEvent);
    }

    protected void sendCloudProcessDeployedEvent(List<CloudProcessDeployedEvent> cloudProcessDeployedEvents) {
        CloudRuntimeEvent<?, ?>[] payload = cloudProcessDeployedEvents.toArray(new CloudRuntimeEvent<?, ?>[]{ });

        Message<?> message = runtimeBundleMessageBuilderFactory.create()
                                                               .withPayload(payload)
                                                               .build();
        streamBridge.send(AUDIT_PRODUCER_OUTPUT_BINDING, message);
    }

    protected List<CloudProcessDeployedEvent> toCloudProcessDeployedEvents(List<ProcessDeployedEvent> processDeployedEvents) {
        return processDeployedEvents.stream()
                                    .map(this::toCloudProcessDeployedEvent)
                                    .collect(Collectors.toList());
    }

    protected CloudProcessDeployedEvent toCloudProcessDeployedEvent(ProcessDeployedEvent processDeployedEvent) {
        CloudProcessDeployedEventImpl cloudProcessDeployedEvent = new CloudProcessDeployedEventImpl(processDeployedEvent.getEntity());
        cloudProcessDeployedEvent.setProcessModelContent(processDeployedEvent.getProcessModelContent());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessDeployedEvent);

        return cloudProcessDeployedEvent;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

}
