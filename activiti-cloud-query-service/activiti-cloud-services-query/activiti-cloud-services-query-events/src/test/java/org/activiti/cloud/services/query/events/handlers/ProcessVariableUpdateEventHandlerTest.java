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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.persistence.EntityManager;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
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

    @Mock
    private EntityManager entityManager;

    @Test
    void handleShouldUpdateVariableAndPersistHistoryEntry() {
        //given
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "newValue",
            "procInstId",
            null
        );
        CloudVariableUpdatedEventImpl<String> event = new CloudVariableUpdatedEventImpl<>(
            "eventId",
            System.currentTimeMillis(),
            variable,
            "oldValue"
        );
        event.setMessageId("msg-001");
        event.setSequenceNumber(3);

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(entityManager).persist(captor.capture());

        ProcessVariableHistoryEntity historyEntity = (ProcessVariableHistoryEntity) captor.getValue();
        assertThat(historyEntity.getProcessInstanceId()).isEqualTo("procInstId");
        assertThat(historyEntity.getVariableName()).isEqualTo("var");
        assertThat(historyEntity.getType()).isEqualTo("string");
        assertThat((String) historyEntity.getValue()).isEqualTo("newValue");
        assertThat(historyEntity.isDeleted()).isFalse();
        assertThat(historyEntity.getMessageId()).isEqualTo("msg-001");
        assertThat(historyEntity.getSequenceNumber()).isEqualTo(3);
        assertThat(historyEntity.getCreateTime()).isNotNull();
    }

    @Test
    void handleShouldSkipHistoryWhenVariableIsEphemeral() {
        //given
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>("var", "string", "newValue", "procInstId", null);
        CloudVariableUpdatedEventImpl<String> event = new CloudVariableUpdatedEventImpl<>(variable, "oldValue", true);

        //when
        handler.handle(event);

        //then
        verifyNoInteractions(entityManager);
    }
}
