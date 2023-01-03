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

import org.activiti.api.process.runtime.events.ProcessCandidateStarterGroupAddedEvent;
import org.activiti.api.runtime.event.impl.ProcessCandidateStarterGroupAddedEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterGroupAddedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupAddedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.springframework.context.event.EventListener;

public class CloudProcessCandidateStarterUserAddedProducer {

    private ProcessEngineChannels producer;
    private RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory;
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public CloudProcessCandidateStarterUserAddedProducer(ProcessEngineChannels producer,
                                                         RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory,
                                                         RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        this.producer = producer;
        this.runtimeBundleMessageBuilderFactory = runtimeBundleMessageBuilderFactory;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    @EventListener
    public void sendProcessCandidateStarterGroupAddedEvents(ProcessCandidateStarterGroupAddedEvents events) {
        producer.auditProducer().send(
            runtimeBundleMessageBuilderFactory.create()
                .withPayload(
                    events.getEvents()
                        .stream()
                        .map(event -> toCloudEvent(event))
                        .toArray(CloudRuntimeEvent<?, ?>[]::new))
                .build());
    }

    public CloudProcessCandidateStarterGroupAddedEvent toCloudEvent(ProcessCandidateStarterGroupAddedEvent event) {
        CloudProcessCandidateStarterGroupAddedEventImpl cloudProcessCandidateStarterGroupAddedEvent = new CloudProcessCandidateStarterGroupAddedEventImpl(event.getEntity());
        cloudProcessCandidateStarterGroupAddedEvent.setProcessDefinitionId(cloudProcessCandidateStarterGroupAddedEvent.getEntity().getProcessDefinitionId());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessCandidateStarterGroupAddedEvent);
        return cloudProcessCandidateStarterGroupAddedEvent;
    }
}
