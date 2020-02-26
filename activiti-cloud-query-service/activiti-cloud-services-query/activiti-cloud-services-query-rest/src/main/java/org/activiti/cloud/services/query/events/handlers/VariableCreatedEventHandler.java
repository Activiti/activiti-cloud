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

import com.querydsl.core.types.dsl.BooleanExpression;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariableCreatedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariableCreatedEventHandler.class);

    private final VariableRepository variableRepository;
    private final TaskVariableRepository taskVariableRepository;
    private final EntityManager entityManager;

    public VariableCreatedEventHandler(VariableRepository variableRepository,
                                       TaskVariableRepository taskVariableRepository,
                                       EntityManager entityManager) {
        this.variableRepository = variableRepository;
        this.taskVariableRepository = taskVariableRepository;
        this.entityManager = entityManager;
    }
    
    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudVariableCreatedEvent variableCreatedEvent = (CloudVariableCreatedEvent) event;
        LOGGER.debug("Handling variableEntity created event: " + variableCreatedEvent.getEntity().getName());
        
        try {
            if (variableCreatedEvent.getEntity().isTaskVariable()) {           
                createTaskVariableEntity(variableCreatedEvent); 
            } else { 
                createProcessVariableEntity(variableCreatedEvent); 
            }
        
        } catch (Exception cause) {
            LOGGER.debug("Error handling VariableCreatedEvent[" + event + "]",
                         cause);
        }
   
    }
   
    private void createTaskVariableEntity(CloudVariableCreatedEvent variableCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity= getProcessInstance(variableCreatedEvent);
        
        String taskId = variableCreatedEvent.getEntity().getTaskId();
        String variableName = variableCreatedEvent.getEntity().getName();
        TaskEntity taskEntity = entityManager.getReference(TaskEntity.class,
                                                           taskId);
           
        BooleanExpression predicate = QTaskVariableEntity.taskVariableEntity.name.eq(variableName)
                .and(
                        QTaskVariableEntity.taskVariableEntity.taskId.eq(taskId)
                );

        if (taskVariableRepository.exists(predicate)) {
            LOGGER.debug("Variable " + variableName + " already exists in the task " + taskId + "!");
            return;
        }
        
        TaskVariableEntity taskVariableEntity = new TaskVariableEntity(null, 
                                                           variableCreatedEvent.getEntity().getType(),
                                                           variableName,
                                                           variableCreatedEvent.getEntity().getProcessInstanceId(),
                                                           variableCreatedEvent.getServiceName(),
                                                           variableCreatedEvent.getServiceFullName(),
                                                           variableCreatedEvent.getServiceVersion(),
                                                           variableCreatedEvent.getAppName(),
                                                           variableCreatedEvent.getAppVersion(),
                                                           taskId,
                                                           new Date(variableCreatedEvent.getTimestamp()),
                                                           new Date(variableCreatedEvent.getTimestamp()),
                                                           null);
        taskVariableEntity.setValue(variableCreatedEvent.getEntity().getValue());
                  
        taskVariableEntity.setProcessInstance(processInstanceEntity);
        
        taskVariableEntity.setTask(taskEntity);
    
        taskVariableRepository.save(taskVariableEntity);
    }
    
    private void createProcessVariableEntity(CloudVariableCreatedEvent variableCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity= getProcessInstance(variableCreatedEvent);
        String processInstanceId = variableCreatedEvent.getEntity().getProcessInstanceId();
        String variableName = variableCreatedEvent.getEntity().getName();
        
        BooleanExpression predicate = QProcessVariableEntity.processVariableEntity.name.eq(variableName)
                .and(
                        QProcessVariableEntity.processVariableEntity.processInstanceId.eq(processInstanceId)
                );
        
        if (variableRepository.exists(predicate)) {
            LOGGER.debug("Variable " + variableName + " already exists in the process " + processInstanceId + "!");
            return;
        }
 
        ProcessVariableEntity variableEntity = new ProcessVariableEntity(null, 
                                                           variableCreatedEvent.getEntity().getType(),
                                                           variableName,
                                                           processInstanceId,
                                                           variableCreatedEvent.getServiceName(),
                                                           variableCreatedEvent.getServiceFullName(),
                                                           variableCreatedEvent.getServiceVersion(),
                                                           variableCreatedEvent.getAppName(),
                                                           variableCreatedEvent.getAppVersion(),
                                                           new Date(variableCreatedEvent.getTimestamp()),
                                                           new Date(variableCreatedEvent.getTimestamp()),
                                                           null);
        variableEntity.setValue(variableCreatedEvent.getEntity().getValue());
                  
        variableEntity.setProcessInstance(processInstanceEntity);
 
        variableRepository.save(variableEntity);
    }
    
    
    private ProcessInstanceEntity getProcessInstance(CloudVariableCreatedEvent variableCreatedEvent) {
        if(variableCreatedEvent.getEntity().getProcessInstanceId() == null){
            return null;
        }else {
            return entityManager
                    .getReference(ProcessInstanceEntity.class,
                            variableCreatedEvent.getEntity().getProcessInstanceId());
        }
    }

    @Override
    public String getHandledEvent() {
        return VariableEvent.VariableEvents.VARIABLE_CREATED.name();
    }
}
