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

import java.util.Objects;
import org.activiti.api.runtime.event.impl.ApplicationDeployedEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudApplicationDeployedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;

public class CloudApplicationDeployedProducer {

    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private ProcessEngineChannels producer;
    private RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory;
    private StreamBridge streamBridge;
    private String auditProducerBindingName;

    public CloudApplicationDeployedProducer(RuntimeBundleInfoAppender runtimeBundleInfoAppender,
            ProcessEngineChannels producer,
            RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.producer = producer;
        this.runtimeBundleMessageBuilderFactory = runtimeBundleMessageBuilderFactory;
    }

    public CloudApplicationDeployedProducer(RuntimeBundleInfoAppender runtimeBundleInfoAppender,
            RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory, StreamBridge streamBridge, String auditProducerBindingName) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.runtimeBundleMessageBuilderFactory = runtimeBundleMessageBuilderFactory;
        this.streamBridge = streamBridge;
        this.auditProducerBindingName = auditProducerBindingName;
        this.producer = null;
    }

    @EventListener
    public void sendApplicationDeployedEvents(ApplicationDeployedEvents applicationDeployedEvents) {
        if (Objects.nonNull(producer)) {
            producer.auditProducer().send(this.getApplicationDeployedEventsMessage(applicationDeployedEvents));
        } else {
            streamBridge.send(auditProducerBindingName, this.getApplicationDeployedEventsMessage(applicationDeployedEvents));
        }
    }

    private Message<CloudRuntimeEvent<?, ?>[]> getApplicationDeployedEventsMessage(ApplicationDeployedEvents applicationDeployedEvents) {
        return runtimeBundleMessageBuilderFactory.create()
                .withPayload(
                        applicationDeployedEvents.getApplicationDeployedEvents()
                                .stream()
                                .map(applicationDeployedEvent -> {
                                    CloudApplicationDeployedEventImpl cloudApplicationDeployedEvent = new CloudApplicationDeployedEventImpl(
                                            applicationDeployedEvent.getEntity());
                                    runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudApplicationDeployedEvent);
                                    return cloudApplicationDeployedEvent;
                                })
                                .toArray(CloudRuntimeEvent<?, ?>[]::new))
                .build();
    }
}
