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
import java.util.Optional;

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.engine.ActivitiException;
import org.activiti.runtime.api.event.CloudProcessResumedEvent;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessResumedEventHandler implements QueryEventHandler {

    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    public ProcessResumedEventHandler(ProcessInstanceRepository processInstanceRepository) {
        this.processInstanceRepository = processInstanceRepository;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessResumedEvent processResumedEvent = (CloudProcessResumedEvent) event;
        String processInstanceId = processResumedEvent.getEntity().getId();
        Optional<ProcessInstance> findResult = processInstanceRepository.findById(processInstanceId);
        ProcessInstance processInstance = findResult.orElseThrow(() -> new ActivitiException("Unable to find process instance with the given id: " + processInstanceId));
        processInstance.setStatus("RUNNING");
        processInstance.setLastModified(new Date(processResumedEvent.getTimestamp()));
        processInstance.setProcessDefinitionKey(processResumedEvent.getEntity().getProcessDefinitionKey());
        processInstance.setInitiator(processResumedEvent.getEntity().getInitiator());
        processInstance.setStartDate(processResumedEvent.getEntity().getStartDate());
        processInstance.setBusinessKey(processResumedEvent.getEntity().getBusinessKey());
        processInstanceRepository.save(processInstance);
    }

    @Override
    public String getHandledEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name();
    }
}
