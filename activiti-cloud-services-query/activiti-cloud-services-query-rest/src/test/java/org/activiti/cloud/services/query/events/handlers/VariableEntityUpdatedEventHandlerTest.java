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

import org.activiti.cloud.services.query.model.VariableEntity;
import org.activiti.runtime.api.event.VariableEvent;
import org.activiti.runtime.api.event.impl.CloudVariableUpdatedEventImpl;
import org.activiti.runtime.api.model.impl.VariableInstanceImpl;
import org.activiti.test.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class VariableEntityUpdatedEventHandlerTest {

    @InjectMocks
    private VariableUpdatedEventHandler handler;

    @Mock
    private ProcessVariableUpdateEventHandler processVariableUpdateEventHandler;

    @Mock
    private TaskVariableUpdatedEventHandler taskVariableUpdatedEventHandler;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldUseProcessVariableUpdateHandlerWhenNoTaskId() {
        //given
        CloudVariableUpdatedEventImpl event = new CloudVariableUpdatedEventImpl(buildVariable());
        event.setServiceName("runtime-bundle-a");

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<VariableEntity> captor = ArgumentCaptor.forClass(VariableEntity.class);
        verify(processVariableUpdateEventHandler).handle(captor.capture());

        VariableEntity variableEntity = captor.getValue();
        Assertions.assertThat(variableEntity)
                .hasProcessInstanceId(event.getEntity().getProcessInstanceId())
                .hasName("var")
                .hasServiceName("runtime-bundle-a")
                .hasValue("v1")
                .hasType("string");
    }

    private VariableInstanceImpl<String> buildVariable() {
        return new VariableInstanceImpl<>("var",
                                          "string",
                                          "v1",
                                          "10");
    }

    @Test
    public void handleShouldUseTaskVariableUpdateHandlerWhenTaskIdIsSet() {
        //given
        VariableInstanceImpl<String> variableInstance = buildVariable();
        variableInstance.setTaskId("20");
        CloudVariableUpdatedEventImpl event = new CloudVariableUpdatedEventImpl(variableInstance);
        event.setServiceName("runtime-bundle-a");

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<VariableEntity> captor = ArgumentCaptor.forClass(VariableEntity.class);
        verify(taskVariableUpdatedEventHandler).handle(captor.capture());

        VariableEntity variableEntity = captor.getValue();
        Assertions.assertThat(variableEntity)
                .hasTaskId("20")
                .hasName("var")
                .hasValue("v1")
                .hasServiceName("runtime-bundle-a")
                .hasType("string");
    }

    @Test
    public void getHandledEventClassShouldReturnVariableUpdatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(VariableEvent.VariableEvents.VARIABLE_UPDATED.name());
    }
}