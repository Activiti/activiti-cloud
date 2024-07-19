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

import jakarta.persistence.EntityManager;
import java.util.Set;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessVariableCreatedEventHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(ProcessVariableCreatedEventHandler.class);

    private final EntityManager entityManager;
    private final EntityManagerFinder entityManagerFinder;

    public ProcessVariableCreatedEventHandler(EntityManager entityManager, EntityManagerFinder entityManagerFinder) {
        this.entityManager = entityManager;
        this.entityManagerFinder = entityManagerFinder;
    }

    public void handle(CloudVariableCreatedEvent variableCreatedEvent) {
        String processInstanceId = variableCreatedEvent.getEntity().getProcessInstanceId();
        String variableName = variableCreatedEvent.getEntity().getName();

        entityManagerFinder
            .findProcessInstanceWithVariables(processInstanceId)
            .ifPresent(processInstanceEntity -> {
                processInstanceEntity
                    .getVariable(variableName)
                    .ifPresentOrElse(
                        variableEntity -> {
                            LOGGER.warn(
                                "Variable " + variableName + " already exists in the process " + processInstanceId + "!"
                            );
                        },
                        () -> {
                            ProcessVariableInstance variableEntity = createProcessVariableInstance(
                                variableCreatedEvent,
                                processInstanceEntity
                            );
                            //TODO fix
                            //processInstanceEntity.getVariables().add(variableEntity);
                            assignToTasks(processInstanceId, variableName, variableEntity);
                        }
                    );
            });
    }

    private ProcessVariableInstance createProcessVariableInstance(
        CloudVariableCreatedEvent variableCreatedEvent,
        ProcessInstanceEntity processInstanceEntity
    ) {
        //TODO fix test
        ProcessVariableInstance variableEntity = new ProcessVariableInstance(
            /*   null,
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
            null */
        );
        variableEntity.setValue(variableCreatedEvent.getEntity().getValue());
        //variableEntity.setVariableDefinitionId(variableCreatedEvent.getVariableDefinitionId());
        variableEntity.setProcessDefinitionKey(variableCreatedEvent.getProcessDefinitionKey());
        //variableEntity.setProcessInstance(processInstanceEntity);

        entityManager.persist(variableEntity);

        return variableEntity;
    }

    private void assignToTasks(String processInstanceId, String variableName, ProcessVariableInstance variableEntity) {
        entityManagerFinder
            .findTasksWithProcessVariables(processInstanceId)
            .forEach(taskEntity -> {
                Set<ProcessVariableInstance> processVariables = taskEntity.getProcessVariables();
                if (processVariables.stream().map(ProcessVariableInstance::getName).anyMatch(variableName::equals)) {
                    LOGGER.warn(
                        "Process variable " + variableName + " already exists in the task " + taskEntity.getId() + "!"
                    );
                } else {
                    taskEntity.getProcessVariables().add(variableEntity);
                }
            });
    }
}
