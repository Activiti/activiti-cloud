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

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessUpdatedEvent;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;

public class ProcessUpdatedEventHandler implements QueryEventHandler {

    private ProcessInstanceRepository processInstanceRepository;

    public ProcessUpdatedEventHandler(ProcessInstanceRepository processInstanceRepository) {
        this.processInstanceRepository = processInstanceRepository;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessUpdatedEvent updatedEvent = (CloudProcessUpdatedEvent) event;
        
        ProcessInstance eventProcessInstance = updatedEvent.getEntity();

        ProcessInstanceEntity processInstanceEntity = processInstanceRepository.findById(eventProcessInstance.getId())
                .orElseThrow(
                        () -> new QueryException("Unable to find process instance with the given id: " + eventProcessInstance.getId())
                );
                
        processInstanceEntity.setBusinessKey(eventProcessInstance.getBusinessKey());
        processInstanceEntity.setName(eventProcessInstance.getName());
        processInstanceEntity.setLastModified(new Date(updatedEvent.getTimestamp()));
        processInstanceRepository.save(processInstanceEntity);
    }

    @Override
    public String getHandledEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED.name();
    }
}
