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

package org.activiti.cloud.services.events.converter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventConverterContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventConverterContext.class);

    private Map<String, EventConverter> convertersMap;

    public EventConverterContext(Map<String, EventConverter> convertersMap) {
        this.convertersMap = convertersMap;
    }

    @Autowired
    public EventConverterContext(Set<EventConverter> converters) {
        this.convertersMap = converters.stream().collect(Collectors.toMap(EventConverter::handledType,
                                                                          Function.identity()));
    }

    Map<String, EventConverter> getConvertersMap() {
        return Collections.unmodifiableMap(convertersMap);
    }

    public ProcessEngineEvent from(ActivitiEvent activitiEvent) {
        EventConverter converter = convertersMap.get(getPrefix(activitiEvent) + activitiEvent.getType());

        ProcessEngineEvent newEvent = null;
        if (converter != null) {
            newEvent = converter.from(activitiEvent);
        } else {
            LOGGER.debug(">> Ommited Event Type: " + activitiEvent.getClass().getCanonicalName());
        }
        return newEvent;
    }

    public static String getPrefix(ActivitiEvent activitiEvent) {
        if (activitiEvent instanceof ActivitiEntityEvent) {
            Object entity = ((ActivitiEntityEvent) activitiEvent).getEntity();
            if (entity != null) {
                if (ProcessInstance.class.isAssignableFrom(entity.getClass())) {
                    if (activitiEvent.getType().equals(ActivitiEventType.ENTITY_SUSPENDED) ||
                            activitiEvent.getType().equals(ActivitiEventType.ENTITY_ACTIVATED) ||
                            activitiEvent.getType().equals(ActivitiEventType.ENTITY_CREATED)) {
                        ExecutionEntity executionEntity = (ExecutionEntity) entity;
                        if (executionEntity.isProcessInstanceType()) {
                            return "ProcessInstance:";
                        } else {
                            return "";
                        }
                    }
                    return "ProcessInstance:";
                } else if (entity instanceof Task) {
                    return "Task:";
                } else if (entity instanceof IdentityLink){
                    IdentityLink identityLink = (IdentityLink)entity;
                    if (IdentityLinkType.CANDIDATE.equalsIgnoreCase(identityLink.getType()) && identityLink.getUserId() != null) {
                        return "TaskCandidateUser:";
                    } else if (IdentityLinkType.CANDIDATE.equalsIgnoreCase(identityLink.getType()) && identityLink.getGroupId() != null) {
                        return "TaskCandidateGroup:";
                    } else {
                        return "";
                    }
                }
            }
        }
        return "";
    }
}
