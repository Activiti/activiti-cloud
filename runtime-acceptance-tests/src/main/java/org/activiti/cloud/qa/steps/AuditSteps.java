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

package org.activiti.cloud.qa.steps;

import java.util.Collection;
import java.util.Map;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.Event;
import org.activiti.cloud.qa.model.EventType;
import org.activiti.cloud.qa.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.qa.service.AuditService;
import org.assertj.core.api.Condition;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

/**
 * Audit steps
 */
@EnableRuntimeFeignContext
public class AuditSteps {

    @Autowired
    private AuditService auditService;

    @Step
    public void checkServicesHealth() {
        assertThat(auditService.isServiceUp()).isTrue();
    }

    @Step
    public Map<String, Object> health() {
        return auditService.health();
    }

    @Step
    public Collection<Event> getEventsByProcessInstanceIdAndEventType(String processInstanceId,
                                                                      EventType eventType) throws Exception {
        return auditService.getProcessInstanceEvents(processInstanceId,
                                                     eventType.getType()).getContent();
    }

    @Step
    public void checkProcessInstanceEvent(String processInstanceId,
                                          EventType eventType) throws Exception {

        assertThat(getEventsByProcessInstanceIdAndEventType(processInstanceId,
                                                            eventType)).isNotEmpty();
    }

    @Step
    public void checkProcessInstanceTaskEvent(String processInstanceId,
                                              String taskId,
                                              EventType eventType) throws Exception {

        Collection<Event> events = getEventsByProcessInstanceIdAndEventType(processInstanceId,
                                                                            eventType);

        assertThat(events).isNotEmpty();
        Event resultingEvent = events.iterator().next();
        assertThat(resultingEvent).isNotNull();
        assertThat(resultingEvent.getTask().getId()).isEqualTo(taskId);
    }

    @Step
    public Collection<Event> getEvents() {
        return auditService.getEvents().getContent();
    }

    /**
     * Check if a standalone task was created
     * and assigned to it's creator
     * @param taskId the id of the task (from rb)
     */
    @Step
    public void checkTaskCreatedAndAssignedEvents(String taskId) {

        final Collection<Event> events = getEvents();
        Condition<Event> taskIsMatched = new Condition<Event>() {
            @Override
            public boolean matches(Event event) {

                return event.getTask() != null && taskId.equals(event.getTask().getId());
            }
        };

        assertThat(events).isNotNull()
                .isNotEmpty()
                .filteredOn(taskIsMatched).hasSize(2)
                .extracting("task.id",
                            "eventType")
                .contains(tuple(taskId,
                                EventType.TASK_CREATED),
                          tuple(taskId,
                                EventType.TASK_ASSIGNED));
    }
}
