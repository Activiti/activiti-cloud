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

import org.activiti.cloud.services.events.converter.ToCloudTaskRuntimeEventConverter;
import org.activiti.runtime.api.event.TaskAssigned;
import org.activiti.runtime.api.event.listener.TaskRuntimeEventListener;

public class CloudTaskAssignedProducer implements TaskRuntimeEventListener<TaskAssigned> {

    private ToCloudTaskRuntimeEventConverter converter;
    private ProcessEngineEventsAggregator eventsAggregator;

    public CloudTaskAssignedProducer(ToCloudTaskRuntimeEventConverter converter,
                                     ProcessEngineEventsAggregator eventsAggregator) {
        this.converter = converter;
        this.eventsAggregator = eventsAggregator;
    }

    @Override
    public void onEvent(TaskAssigned event) {
        eventsAggregator.add(converter.from(event));
    }
}
