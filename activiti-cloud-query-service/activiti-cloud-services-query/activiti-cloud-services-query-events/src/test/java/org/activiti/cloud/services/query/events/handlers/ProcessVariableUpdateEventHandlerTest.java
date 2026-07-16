/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessVariableUpdateEventHandlerTest {

    @InjectMocks
    private ProcessVariableUpdateEventHandler handler;

    @Mock
    private ProcessVariableUpdater variableUpdater;

    @Test
    void should_updateVariable_when_handleCalled() {
        var variable = new VariableInstanceImpl<>("var", "string", "newValue", "procInstId", null);
        var event = new CloudVariableUpdatedEventImpl<>("eventId", System.currentTimeMillis(), variable, "oldValue");
        event.setMessageId("msg-001");
        event.setSequenceNumber(3);

        handler.handle(event);

        var captor = ArgumentCaptor.forClass(ProcessVariableEntity.class);
        verify(variableUpdater).update(captor.capture(), anyString());
        assertThat(captor.getValue())
            .extracting(
                ProcessVariableEntity::getName,
                ProcessVariableEntity::getType,
                ProcessVariableEntity::getValue,
                ProcessVariableEntity::getProcessInstanceId
            )
            .containsExactly("var", "string", "newValue", "procInstId");
    }
}
