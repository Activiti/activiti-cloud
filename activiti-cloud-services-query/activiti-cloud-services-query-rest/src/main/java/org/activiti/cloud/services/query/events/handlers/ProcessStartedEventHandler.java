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

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.events.ProcessStartedEvent;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.engine.ActivitiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessStartedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessStartedEventHandler.class);

    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    public ProcessStartedEventHandler(ProcessInstanceRepository processInstanceRepository) {
        this.processInstanceRepository = processInstanceRepository;
    }

    @Override
    public void handle(ProcessEngineEvent startedEvent) {
        LOGGER.debug("Handling start of process Instance " + startedEvent.getProcessInstanceId());

        String processInstanceId = startedEvent.getProcessInstanceId();
        Optional<ProcessInstance> findResult = processInstanceRepository.findById(processInstanceId);
        if (findResult.isPresent()) {
            ProcessInstance processInstance = findResult.get();
            if(processInstance.getStatus() == null || !processInstance.getStatus().equals("CREATED")){
                throw new ActivitiException("Unable to start process instance in wrong status: " + processInstance.getStatus());
            }
            processInstance.setStatus("RUNNING");
            processInstance.setLastModified(new Date(startedEvent.getTimestamp()));
            processInstanceRepository.save(processInstance);
        } else {
            throw new ActivitiException("Unable to find process instance with the given id: " + processInstanceId);
        }

    }

    @Override
    public Class<? extends ProcessEngineEvent> getHandledEventClass() {
        return ProcessStartedEvent.class;
    }

}
