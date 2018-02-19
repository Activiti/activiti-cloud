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

import java.util.Map;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.ActivitiProcessStartedEvent;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.services.events.converter.EventConverterContext.getPrefix;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:test-application.properties")
public class EventConverterContextIT {

    @Autowired
    private EventConverterContext converterContext;

    @Configuration
    @ComponentScan({
            "org.activiti.cloud.services.events.converter",
            "org.activiti.cloud.services.events.configuration",
            "org.activiti.cloud.services.api.model.converter"
    })
    public static class EventConverterContextConfig {

    }

    @Test
    public void shouldHandleAllSupportedEvents() throws Exception {
        //when
        Map<String, EventConverter> converters = converterContext.getConvertersMap();

        //then
        Assertions.assertThat(converters).containsOnlyKeys(ActivitiEventType.ACTIVITY_CANCELLED.toString(),
                                                           ActivitiEventType.ACTIVITY_COMPLETED.toString(),
                                                           ActivitiEventType.ACTIVITY_STARTED.toString(),
                                                           "ProcessInstance:" + ActivitiEventType.PROCESS_STARTED.toString(),
                                                           "ProcessInstance:" + ActivitiEventType.PROCESS_CANCELLED.toString(),
                                                           "ProcessInstance:" + ActivitiEventType.PROCESS_COMPLETED.toString(),
                                                           "ProcessInstance:" + ActivitiEventType.ENTITY_CREATED.toString(),
                                                           "ProcessInstance:" + ActivitiEventType.ENTITY_SUSPENDED.toString(),
                                                           "ProcessInstance:" + ActivitiEventType.ENTITY_ACTIVATED.toString(),
                                                           "Task:" + ActivitiEventType.ENTITY_SUSPENDED.toString(),
                                                           "Task:" + ActivitiEventType.ENTITY_ACTIVATED.toString(),
                                                           "Task:" + ActivitiEventType.TASK_ASSIGNED.toString(),
                                                           "Task:" + ActivitiEventType.TASK_COMPLETED.toString(),
                                                           "Task:" + ActivitiEventType.TASK_CREATED.toString(),
                                                           ActivitiEventType.SEQUENCEFLOW_TAKEN.toString(),
                                                           ActivitiEventType.VARIABLE_CREATED.toString(),
                                                           ActivitiEventType.VARIABLE_DELETED.toString(),
                                                           ActivitiEventType.VARIABLE_UPDATED.toString());
    }

    @Test
    public void shouldIncludeApplicationNameInConvertedEvents() throws Exception {

        //when
        Map<String, EventConverter> converters = converterContext.getConvertersMap();

        //then
        Assertions.assertThat(converters).containsKey("ProcessInstance:" + ActivitiEventType.PROCESS_STARTED.toString());
        ActivitiProcessStartedEvent activitiEvent = mock(ActivitiProcessStartedEvent.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.PROCESS_STARTED);
        ExecutionEntityImpl executionEntity = mock(ExecutionEntityImpl.class);
        ExecutionEntityImpl internalProcessInstance = mock(ExecutionEntityImpl.class);
        given(activitiEvent.getEntity()).willReturn(executionEntity);
        given(executionEntity.getProcessInstance()).willReturn(internalProcessInstance);

        ProcessEngineEvent processEngineEvent = converters.get(getPrefix(activitiEvent) + ActivitiEventType.PROCESS_STARTED).from(activitiEvent);

        assertThat(processEngineEvent).isNotNull();
        // this comes from the application.properties (test-application.properties) spring app name configuration
        assertThat(processEngineEvent.getApplicationName()).isEqualTo("test-app");
    }
}