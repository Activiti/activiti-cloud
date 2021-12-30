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
package org.activiti.cloud.services.query.events.handlers;

import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Date;

public class ProcessVariableCreatedEventHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(ProcessVariableCreatedEventHandler.class);

    private final EntityManager entityManager;

    public ProcessVariableCreatedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void handle(CloudVariableCreatedEvent variableCreatedEvent) {
        String processInstanceId = variableCreatedEvent.getEntity()
                                                       .getProcessInstanceId();
        String variableName = variableCreatedEvent.getEntity()
                                                  .getName();

        ProcessInstanceEntity processInstanceEntity = entityManager.find(ProcessInstanceEntity.class,
                                                                         processInstanceId);
        processInstanceEntity.getVariable(variableName)
                             .ifPresentOrElse(variableEntity -> {
                                 LOGGER.warn("Variable " + variableName + " already exists in the process " + processInstanceId + "!");
                             },
                             () -> {
                                 ProcessVariableEntity variableEntity = createProcessVariableEntity(variableCreatedEvent,
                                                                                                    processInstanceEntity);
                                 processInstanceEntity.getVariables()
                                                      .add(variableEntity);
                             });
    }

    private ProcessVariableEntity createProcessVariableEntity(CloudVariableCreatedEvent variableCreatedEvent,
                                                              ProcessInstanceEntity processInstanceEntity) {
        ProcessVariableEntity variableEntity = new ProcessVariableEntity(null,
                                                                         variableCreatedEvent.getEntity().getType(),
                                                                         variableCreatedEvent.getEntity().getName(),
                                                                         variableCreatedEvent.getEntity().getProcessInstanceId(),
                                                                         variableCreatedEvent.getServiceName(),
                                                                         variableCreatedEvent.getServiceFullName(),
                                                                         variableCreatedEvent.getServiceVersion(),
                                                                         variableCreatedEvent.getAppName(),
                                                                         variableCreatedEvent.getAppVersion(),
                                                                         new Date(variableCreatedEvent.getTimestamp()),
                                                                         new Date(variableCreatedEvent.getTimestamp()),
                                                                         null);
        variableEntity.setValue(variableCreatedEvent.getEntity()
                                                    .getValue());
        variableEntity.setProcessInstance(processInstanceEntity);

        entityManager.persist(variableEntity);

        return variableEntity;
    }
}

