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

import org.activiti.api.process.runtime.events.ProcessCandidateStarterUserAddedEvent;
import org.activiti.api.runtime.event.impl.ProcessCandidateStarterUserAddedEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterUserAddedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserAddedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.springframework.context.event.EventListener;

public class CloudProcessCandidateStarterGroupAddedProducer {

    private ProcessEngineChannels producer;
    private RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory;
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public CloudProcessCandidateStarterGroupAddedProducer(
        ProcessEngineChannels producer,
        RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory,
        RuntimeBundleInfoAppender runtimeBundleInfoAppender
    ) {
        this.producer = producer;
        this.runtimeBundleMessageBuilderFactory = runtimeBundleMessageBuilderFactory;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    @EventListener
    public void sendProcessCandidateStarterUserAddedEvents(ProcessCandidateStarterUserAddedEvents events) {
        producer
            .auditProducer()
            .send(
                runtimeBundleMessageBuilderFactory
                    .create()
                    .withPayload(
                        events
                            .getEvents()
                            .stream()
                            .map(event -> toCloudEvent(event))
                            .toArray(CloudRuntimeEvent<?, ?>[]::new)
                    )
                    .build()
            );
    }

    private CloudProcessCandidateStarterUserAddedEvent toCloudEvent(ProcessCandidateStarterUserAddedEvent event) {
        CloudProcessCandidateStarterUserAddedEventImpl cloudProcessCandidateStarterUserAddedEvent = new CloudProcessCandidateStarterUserAddedEventImpl(
            event.getEntity()
        );
        cloudProcessCandidateStarterUserAddedEvent.setProcessDefinitionId(
            cloudProcessCandidateStarterUserAddedEvent.getEntity().getProcessDefinitionId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessCandidateStarterUserAddedEvent);
        return cloudProcessCandidateStarterUserAddedEvent;
    }
}
