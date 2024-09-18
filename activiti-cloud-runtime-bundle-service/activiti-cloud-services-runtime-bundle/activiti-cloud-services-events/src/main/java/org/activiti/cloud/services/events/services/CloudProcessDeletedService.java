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
package org.activiti.cloud.services.events.services;

import static org.activiti.api.process.model.builders.ProcessPayloadBuilder.delete;

import java.util.List;
import java.util.Optional;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;

public class CloudProcessDeletedService {

    private final ProcessEngineChannels producer;
    private final RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private final ProcessAdminRuntime processAdminRuntime;

    public CloudProcessDeletedService(
        ProcessEngineChannels producer,
        RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory,
        RuntimeBundleInfoAppender runtimeBundleInfoAppender,
        ProcessAdminRuntime processAdminRuntime
    ) {
        this.producer = producer;
        this.runtimeBundleMessageBuilderFactory = runtimeBundleMessageBuilderFactory;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.processAdminRuntime = processAdminRuntime;
    }

    public void sendDeleteEvent(String processInstanceId) {
        Optional
            .ofNullable(processAdminRuntime.delete(delete().withProcessInstanceId(processInstanceId).build()))
            .ifPresent(this::sendEvent);
    }

    protected void sendEvent(ProcessInstance processInstance) {
        producer
            .auditProducer()
            .send(runtimeBundleMessageBuilderFactory.create().withPayload(buildEvents(processInstance)).build());
    }

    protected List<CloudRuntimeEvent<?, ?>> buildEvents(ProcessInstance processInstance) {
        CloudProcessDeletedEventImpl event = new CloudProcessDeletedEventImpl(processInstance);
        return List.of(runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(event));
    }
}
