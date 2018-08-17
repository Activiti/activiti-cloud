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

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.runtime.api.model.impl.VariableInstanceImpl;

public class VariableEventContainedBuilder {

    private VariableInstanceImpl<?> variableInstance;
    private VariableInstanceImpl<?> beforeUpdateVariableInstance;

    private EventsAggregator eventsAggregator;

    public VariableEventContainedBuilder(EventsAggregator eventsAggregator) {
        this.eventsAggregator = eventsAggregator;
    }

    public <T> VariableEventContainedBuilder aCreatedVariable(String name,
                                                              T value,
                                                              String type) {
        variableInstance = buildVariable(name,
                                         type,
                                         value);
        eventsAggregator.addEvents(new CloudVariableCreatedEventImpl(variableInstance));
        return this;
    }

    public <T> VariableEventContainedBuilder anUpdatedVariable(String name,
                                                               T value,
                                                               String type) {
        beforeUpdateVariableInstance = buildVariable(name,
                                         type,
                                         null);
        variableInstance = buildVariable(name,
                                                     type,
                                                     value);
        eventsAggregator.addEvents(new CloudVariableCreatedEventImpl(beforeUpdateVariableInstance),
                                   new CloudVariableUpdatedEventImpl(variableInstance));
        return this;
    }

    public <T> VariableEventContainedBuilder aDeletedVariable(String name,
                                                              T value,
                                                              String type) {
        variableInstance = buildVariable(name,
                                         type,
                                         value);
        eventsAggregator.addEvents(new CloudVariableCreatedEventImpl(variableInstance),
                                   new CloudVariableDeletedEventImpl(variableInstance));
        return this;
    }

    private <T> VariableInstanceImpl<T> buildVariable(String name,
                                                      String type,
                                                      T value) {
        return new VariableInstanceImpl<>(name,
                                          type,
                                          value,
                                          null);
    }

    public VariableInstance onProcessInstance(ProcessInstance processInstance) {

        setProcessInstanceInfo(variableInstance,
                               processInstance);
        setProcessInstanceInfo(beforeUpdateVariableInstance,
                               processInstance);
        return variableInstance;
    }

    private void setProcessInstanceInfo(VariableInstanceImpl<?> variableInstance,
                                        ProcessInstance processInstance) {
        if (variableInstance != null) {
            variableInstance.setProcessInstanceId(processInstance.getId());
        }
    }

    public VariableInstance onTask(Task task) {
        setTaskInfo(variableInstance,
                    task);
        setTaskInfo(beforeUpdateVariableInstance, task);
        return variableInstance;
    }

    private void setTaskInfo(VariableInstanceImpl<?> variableInstance,
                             Task task) {
        if (variableInstance != null) {
            variableInstance.setProcessInstanceId(task.getProcessInstanceId());
            variableInstance.setTaskId(task.getId());
        }
    }
}
