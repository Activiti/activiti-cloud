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

import java.util.UUID;

import org.activiti.runtime.api.event.VariableEvent;
import org.activiti.runtime.api.event.impl.CloudVariableDeletedEventImpl;
import org.activiti.runtime.api.model.impl.VariableInstanceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class VariableEntityDeletedEventHandlerTest {

    @InjectMocks
    private VariableDeletedEventHandler handler;

    @Mock
    private ProcessVariableDeletedEventHandler processVariableDeletedHandler;

    @Mock
    private TaskVariableDeletedEventHandler taskVariableDeletedEventHandler;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldUseProcessVariableDeleteHandlerWhenNoTaskId() {
        //given
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(buildVariable());

        //when
        handler.handle(event);

        //then
        verify(processVariableDeletedHandler).handle(event);
    }

    private VariableInstanceImpl<String> buildVariable() {
        return new VariableInstanceImpl<>("var",
                                          "v1",
                                          "string",
                                          UUID.randomUUID().toString());
    }

    @Test
    public void handleShouldUseProcessVariableDeleteHandlerWhenTaskIdIsPresent() {
        //given
        VariableInstanceImpl<String> variableInstance = buildVariable();
        variableInstance.setTaskId(UUID.randomUUID().toString());
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(variableInstance);

        //when
        handler.handle(event);

        //then
        verify(taskVariableDeletedEventHandler).handle(event);
    }

    @Test
    public void getHandledEventShouldReturnVariableDeletedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(VariableEvent.VariableEvents.VARIABLE_DELETED.name());
    }
}