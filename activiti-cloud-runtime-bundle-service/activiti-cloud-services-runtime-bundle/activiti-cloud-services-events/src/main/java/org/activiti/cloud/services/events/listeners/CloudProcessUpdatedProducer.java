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

import org.activiti.api.process.runtime.events.ProcessUpdatedEvent;
import org.activiti.api.process.runtime.events.listener.ProcessEventListener;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;

public class CloudProcessUpdatedProducer implements ProcessEventListener<ProcessUpdatedEvent> {

    private final ToCloudProcessRuntimeEventConverter eventConverter;
    private final ProcessEngineEventsAggregator eventsAggregator;

    public CloudProcessUpdatedProducer(
        ToCloudProcessRuntimeEventConverter eventConverter,
        ProcessEngineEventsAggregator eventsAggregator
    ) {
        this.eventConverter = eventConverter;
        this.eventsAggregator = eventsAggregator;
    }

    @Override
    public void onEvent(ProcessUpdatedEvent event) {
        eventsAggregator.add(eventConverter.from(event));
    }
}
