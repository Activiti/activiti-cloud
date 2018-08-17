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

package org.activiti.cloud.starters.test.builder;

import java.util.UUID;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.runtime.api.model.impl.ProcessInstanceImpl;

public class ProcessInstanceEventContainedBuilder {

    private final EventsAggregator eventsAggregator;

    public ProcessInstanceEventContainedBuilder(EventsAggregator eventsAggregator) {
        this.eventsAggregator = eventsAggregator;
    }

    public ProcessInstance aCompletedProcessInstance(String name) {
        ProcessInstance processInstance = buildProcessInstance(name);
        eventsAggregator.addEvents(new CloudProcessCreatedEventImpl(processInstance),
                                   new CloudProcessStartedEventImpl(processInstance,
                                                                    null,
                                                                    null),
                                   new CloudProcessCompletedEventImpl(processInstance));
        return processInstance;
    }

    public ProcessInstanceImpl aRunningProcessInstance(String name) {
        ProcessInstanceImpl processInstance = buildProcessInstance(name);
        eventsAggregator.addEvents(new CloudProcessCreatedEventImpl(processInstance),
                                   new CloudProcessStartedEventImpl(processInstance,
                                                       null,
                                                       null));
        return processInstance;
    }

    private ProcessInstanceImpl buildProcessInstance(String name) {
        ProcessInstanceImpl completedProcess = new ProcessInstanceImpl();
        completedProcess.setId(UUID.randomUUID().toString());
        completedProcess.setName(name);
        completedProcess.setProcessDefinitionKey("my-proc");
        completedProcess.setProcessDefinitionId(UUID.randomUUID().toString());
        return completedProcess;
    }

}
