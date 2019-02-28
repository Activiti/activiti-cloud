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

import org.activiti.api.runtime.event.impl.ProcessDeployedEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.springframework.context.event.EventListener;

public class CloudProcessDeployedProducer {

    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private ProcessEngineChannels producer;
    private RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory;

    public CloudProcessDeployedProducer(RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                        ProcessEngineChannels producer,
                                        RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.producer = producer;
        this.runtimeBundleMessageBuilderFactory = runtimeBundleMessageBuilderFactory;
    }

    @EventListener
    public void sendProcessDeployedEvents(ProcessDeployedEvents processDeployedEvents) {
        producer.auditProducer().send(
                runtimeBundleMessageBuilderFactory.create()
                        .withPayload(
                                processDeployedEvents.getProcessDeployedEvents()
                                        .stream()
                                        .map(processDeployedEvent -> {
                                            CloudProcessDeployedEventImpl cloudProcessDeployedEvent = new CloudProcessDeployedEventImpl(processDeployedEvent.getEntity());
                                            cloudProcessDeployedEvent.setProcessModelContent(processDeployedEvent.getProcessModelContent());
                                            runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessDeployedEvent);
                                            return cloudProcessDeployedEvent;
                                        })
                                        .toArray(CloudRuntimeEvent<?, ?>[]::new))
                        .build());
    }
}
