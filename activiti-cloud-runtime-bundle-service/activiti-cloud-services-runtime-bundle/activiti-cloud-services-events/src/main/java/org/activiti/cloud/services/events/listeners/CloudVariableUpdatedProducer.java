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

import org.activiti.api.model.shared.event.VariableUpdatedEvent;
import org.activiti.api.runtime.shared.events.VariableEventListener;
import org.activiti.cloud.services.events.converter.ToCloudVariableEventConverter;

public class CloudVariableUpdatedProducer implements VariableEventListener<VariableUpdatedEvent> {

    private ToCloudVariableEventConverter converter;
    private ProcessEngineEventsAggregator eventsAggregator;

    public CloudVariableUpdatedProducer(
        ToCloudVariableEventConverter converter,
        ProcessEngineEventsAggregator eventsAggregator
    ) {
        this.converter = converter;
        this.eventsAggregator = eventsAggregator;
    }

    @Override
    public void onEvent(VariableUpdatedEvent event) {
        eventsAggregator.add(converter.from(event));
    }
}
