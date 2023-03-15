/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VariableEntityDeletedEventHandlerTest {

    @InjectMocks
    private VariableDeletedEventHandler handler;

    @Mock
    private ProcessVariableDeletedEventHandler processVariableDeletedHandler;

    @Mock
    private TaskVariableDeletedEventHandler taskVariableDeletedEventHandler;

    @Test
    public void handleShouldUseProcessVariableDeleteHandlerWhenNoTaskId() {
        //given
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(buildVariable());

        //when
        handler.handle(event);

        //then
        verify(processVariableDeletedHandler).handle(event);
    }

    private static VariableInstanceImpl<String> buildVariable() {
        return new VariableInstanceImpl<>("var", "v1", "string", "procInstId", null);
    }

    @Test
    public void handleShouldUseProcessVariableDeleteHandlerWhenTaskIdIsPresent() {
        //given
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(buildVariableWithTaskId());

        //when
        handler.handle(event);

        //then
        verify(taskVariableDeletedEventHandler).handle(event);
    }

    private static VariableInstanceImpl<String> buildVariableWithTaskId() {
        return new VariableInstanceImpl<>("var", "v1", "string", "procInstId", "taskId");
    }

    @Test
    public void getHandledEventShouldReturnVariableDeletedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(VariableEvent.VariableEvents.VARIABLE_DELETED.name());
    }
}
