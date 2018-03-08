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

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.Event;
import org.activiti.cloud.qa.model.EventType;
import org.activiti.cloud.qa.rest.feign.EnableFeignContext;
import org.activiti.cloud.qa.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

/**
 * Audit steps
 */
@EnableFeignContext
public class AuditSteps {

    @Autowired
    private AuditService auditService;

    @Step
    public Collection<Event> getEventsByProcessInstanceIdAndEventType(String processInstanceId,
                                                                      EventType eventType) throws Exception {
        return auditService.getEvents(processInstanceId,
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
}
