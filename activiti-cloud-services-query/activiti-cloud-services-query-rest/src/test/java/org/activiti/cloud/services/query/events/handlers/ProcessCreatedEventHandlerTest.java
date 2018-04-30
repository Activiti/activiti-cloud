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
import org.activiti.cloud.services.query.events.ProcessCreatedEvent;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.test.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessCreatedEventHandlerTest {

    @InjectMocks
    private ProcessCreatedEventHandler handler;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateCurrentProcessInstanceStateToCreated() throws Exception {
        //given
        ProcessInstance currentProcessInstance = mock(ProcessInstance.class);
        given(currentProcessInstance.getProcessDefinitionKey()).willReturn("mykey");
        ProcessCreatedEvent event = new ProcessCreatedEvent(System.currentTimeMillis(),
                                                            "ProcessCreatedEvent",
                                                            "10",
                                                            "100",
                                                            "200",
                "runtime-bundle-a",
                "runtime-bundle-a",
                "runtime-bundle",
                "1",
                null,
                null,
                                                            currentProcessInstance);


        //when
        handler.handle(event);

        ArgumentCaptor<ProcessInstance> argumentCaptor = ArgumentCaptor.forClass(ProcessInstance.class);
        verify(processInstanceRepository).save(argumentCaptor.capture());

        ProcessInstance processInstance = argumentCaptor.getValue();
        Assertions.assertThat(processInstance)
                .hasId("200")
                .hasProcessDefinitionId("100")
                .hasServiceName("runtime-bundle-a")
                .hasProcessDefinitionKey("mykey")
                .hasStatus("CREATED");
    }

    @Test
    public void getHandledEventClassShouldReturnProcessCreatedEvent() throws Exception {
        //when
        Class<? extends ProcessEngineEvent> handledEventClass = handler.getHandledEventClass();

        //then
        assertThat(handledEventClass).isEqualTo(ProcessCreatedEvent.class);
    }
}