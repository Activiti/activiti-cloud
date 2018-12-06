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

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VariableCreatedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariableCreatedEventHandler.class);

    private final VariableRepository variableRepository;
    private final TaskVariableRepository taskVariableRepository;
    private final EntityManager entityManager;

    @Autowired
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
            throw new QueryException("Error handling VariableCreatedEvent[" + event + "]",
                                     cause);
        }
   
    }
   
    private void createTaskVariableEntity(CloudVariableCreatedEvent variableCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity= getProcessInstance(variableCreatedEvent);
        TaskEntity taskEntity = entityManager.getReference(TaskEntity.class,
                                                           variableCreatedEvent.getEntity().getTaskId());
        
        TaskVariableEntity taskVariableEntity = new TaskVariableEntity(null, 
                                                           variableCreatedEvent.getEntity().getType(),
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
                                                           null);
        taskVariableEntity.setValue(variableCreatedEvent.getEntity().getValue());
                  
        taskVariableEntity.setProcessInstance(processInstanceEntity);
        
        taskVariableEntity.setTask(taskEntity);
    
        taskVariableRepository.save(taskVariableEntity);
 
    }
    
    private void createProcessVariableEntity(CloudVariableCreatedEvent variableCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity= getProcessInstance(variableCreatedEvent);
        
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
        variableEntity.setValue(variableCreatedEvent.getEntity().getValue());
                  
        variableEntity.setProcessInstance(processInstanceEntity);
  
        variableRepository.save(variableEntity);
    }
    
    private ProcessInstanceEntity getProcessInstance(CloudVariableCreatedEvent variableCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity = entityManager
                .getReference(ProcessInstanceEntity.class,
                              variableCreatedEvent.getEntity().getProcessInstanceId());
        return processInstanceEntity;
    }

    @Override
    public String getHandledEvent() {
        return VariableEvent.VariableEvents.VARIABLE_CREATED.name();
    }
}
