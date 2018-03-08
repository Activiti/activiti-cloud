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
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.activiti.engine.task.IdentityLinkType.CANDIDATE;

@Component
public class EventConverterContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventConverterContext.class);

    public static final String PROCESS_EVENT_PREFIX = "ProcessInstance:";
    public static final String TASK_EVENT_PREFIX = "Task:";
    public static final String TASK_CANDIDATE_USER_EVENT_PREFIX = "TaskCandidateUser:";
    public static final String TASK_CANDIDATE_GROUP_EVENT_PREFIX = "TaskCandidateGroup:";
    public static final String ACTIVITI_EVENT_PREFIX = "";

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
        if (isProcessEvent(activitiEvent)) {
            return PROCESS_EVENT_PREFIX;
        }

        if (isTaskEvent(activitiEvent)) {
            return TASK_EVENT_PREFIX;
        }

        if (isIdentityLinkEntityEvent(activitiEvent)) {
            IdentityLink identityLinkEntity = (IdentityLink) ((ActivitiEntityEvent) activitiEvent).getEntity();
            if (isCandidateUserEntity(identityLinkEntity)) {
                return TASK_CANDIDATE_USER_EVENT_PREFIX;
            }

            if (isCandidateGroupEntity(identityLinkEntity)) {
                return TASK_CANDIDATE_GROUP_EVENT_PREFIX;
            }
        }

        return ACTIVITI_EVENT_PREFIX;
    }

    private static boolean isProcessEvent(ActivitiEvent activitiEvent) {
        if (activitiEvent instanceof ActivitiEntityEvent) {
            Object entity = ((ActivitiEntityEvent) activitiEvent).getEntity();
            if (entity != null && ProcessInstance.class.isAssignableFrom(entity.getClass())) {
                return isExecutionEntityEvent(activitiEvent) ?
                        ((ExecutionEntity) entity).isProcessInstanceType() :
                        true;
            }
        }

        return activitiEvent.getType() == ActivitiEventType.PROCESS_CANCELLED;
    }

    private static boolean isExecutionEntityEvent(ActivitiEvent activitiEvent) {
        return activitiEvent.getType() == ActivitiEventType.ENTITY_SUSPENDED ||
                activitiEvent.getType() == ActivitiEventType.ENTITY_ACTIVATED ||
                activitiEvent.getType() == ActivitiEventType.ENTITY_CREATED;
    }

    private static boolean isTaskEvent(ActivitiEvent activitiEvent) {
        return activitiEvent instanceof ActivitiEntityEvent &&
                ((ActivitiEntityEvent) activitiEvent).getEntity() instanceof Task;
    }

    private static boolean isIdentityLinkEntityEvent(ActivitiEvent activitiEvent) {
        return activitiEvent instanceof ActivitiEntityEvent &&
                ((ActivitiEntityEvent) activitiEvent).getEntity() instanceof IdentityLink;
    }

    private static boolean isCandidateUserEntity(IdentityLink identityLinkEntity) {
        return CANDIDATE.equalsIgnoreCase(identityLinkEntity.getType()) &&
                identityLinkEntity.getUserId() != null;
    }

    private static boolean isCandidateGroupEntity(IdentityLink identityLinkEntity) {
        return CANDIDATE.equalsIgnoreCase(identityLinkEntity.getType()) &&
                identityLinkEntity.getGroupId() != null;
    }
}
