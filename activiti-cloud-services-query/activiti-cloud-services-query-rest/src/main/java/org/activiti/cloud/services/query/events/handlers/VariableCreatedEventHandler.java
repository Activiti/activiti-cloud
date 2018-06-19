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

package org.activiti.cloud.services.query.events.handlers;

import java.util.Date;
import javax.persistence.EntityManager;

import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.cloud.services.query.model.Task;
import org.activiti.cloud.services.query.model.Variable;
import org.activiti.engine.ActivitiException;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudVariableCreatedEvent;
import org.activiti.runtime.api.event.VariableEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VariableCreatedEventHandler implements QueryEventHandler {

    private final VariableRepository variableRepository;

    private final EntityManager entityManager;

    @Autowired
    public VariableCreatedEventHandler(VariableRepository variableRepository,
                                       EntityManager entityManager) {
        this.variableRepository = variableRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudVariableCreatedEvent variableCreatedEvent = (CloudVariableCreatedEvent) event;
        Variable variable = new Variable(variableCreatedEvent.getEntity().getType(),
                                         variableCreatedEvent.getEntity().getName(),
                                         variableCreatedEvent.getEntity().getProcessInstanceId(),
                                         variableCreatedEvent.getServiceName(),
                                         variableCreatedEvent.getServiceFullName(),
                                         variableCreatedEvent.getServiceVersion(),
                                         variableCreatedEvent.getAppName(),
                                         variableCreatedEvent.getAppVersion(),
                                         variableCreatedEvent.getEntity().getTaskId(),
                                         new Date(variableCreatedEvent.getTimestamp()),
                                         new Date(variableCreatedEvent.getTimestamp()),
                                         null,
                                         variableCreatedEvent.getEntity().getValue());

        setProcessInstance(variableCreatedEvent,
                           variable);

        setTask(variableCreatedEvent,
                variable);

        persist(event,
                variable);
    }

    private void persist(CloudRuntimeEvent<?, ?> event,
                         Variable variable) {
        try {
            variableRepository.save(variable);
        } catch (Exception cause) {
            throw new ActivitiException("Error handling VariableCreatedEvent[" + event + "]",
                                        cause);
        }
    }

    private void setTask(CloudVariableCreatedEvent variableCreatedEvent,
                         Variable variable) {
        if (variableCreatedEvent.getEntity().isTaskVariable()) {
            Task task = entityManager.getReference(Task.class,
                                                   variableCreatedEvent.getEntity().getTaskId());
            variable.setTask(task);
        }
    }

    private void setProcessInstance(CloudVariableCreatedEvent variableCreatedEvent,
                                    Variable variable) {
        ProcessInstance processInstance = entityManager
                .getReference(ProcessInstance.class,
                              variableCreatedEvent.getEntity().getProcessInstanceId());

        variable.setProcessInstance(processInstance);
    }

    @Override
    public String getHandledEvent() {
        return VariableEvent.VariableEvents.VARIABLE_CREATED.name();
    }
}
