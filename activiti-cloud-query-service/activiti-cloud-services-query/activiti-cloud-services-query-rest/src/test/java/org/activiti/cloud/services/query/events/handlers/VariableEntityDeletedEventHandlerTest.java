/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.events.handlers;

import java.util.UUID;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
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
