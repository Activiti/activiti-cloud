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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessVariableDeletedEventHandlerTest {

    @InjectMocks
    private ProcessVariableDeletedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    @Mock
    private FeatureToggle featureToggle;

    @Test
    void handleShouldRemoveVariableAndPersistHistoryEntryAsDeleted() {
        //given
        when(featureToggle.isEnabled(QueryFeatureToggles.PROCESS_VARIABLE_HISTORY)).thenReturn(true);
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(
            "eventId",
            System.currentTimeMillis(),
            variable
        );
        event.setMessageId("msg-002");
        event.setSequenceNumber(5);

        ProcessVariableEntity variableEntity = new ProcessVariableEntity();
        variableEntity.setName("var");

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setStatus(ProcessInstanceStatus.RUNNING);
        processInstanceEntity.getVariables().add(variableEntity);

        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .thenReturn(Optional.of(processInstanceEntity));

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<ProcessVariableHistoryEntity> historyCaptor = ArgumentCaptor.forClass(
            ProcessVariableHistoryEntity.class
        );
        verify(entityManager).persist(historyCaptor.capture());

        ProcessVariableHistoryEntity historyEntity = historyCaptor.getValue();
        assertThat(historyEntity.getProcessInstanceId()).isEqualTo("procInstId");
        assertThat(historyEntity.getVariableName()).isEqualTo("var");
        assertThat(historyEntity.getType()).isEqualTo("string");
        assertThat(historyEntity.isDeleted()).isTrue();
        assertThat((Object) historyEntity.getValue()).isNull();
        assertThat(historyEntity.getMessageId()).isEqualTo("msg-002");
        assertThat(historyEntity.getSequenceNumber()).isEqualTo(5);
        assertThat(historyEntity.getEventTime()).isNotNull();
        assertThat(historyEntity.getRecordCreateTime()).isNotNull();

        verify(entityManager).remove(variableEntity);
        assertThat(processInstanceEntity.getVariables()).doesNotContain(variableEntity);
    }

    @Test
    void handleShouldSkipWhenProcessInstanceIsInFinalState() {
        //given
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(variable);

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setStatus(ProcessInstanceStatus.COMPLETED);

        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .thenReturn(Optional.of(processInstanceEntity));

        //when
        handler.handle(event);

        //then - no history persisted and no remove called
        verifyNoInteractions(entityManager);
    }

    @Test
    void handleShouldSkipWhenProcessInstanceNotFound() {
        //given
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(variable);

        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId")).thenReturn(Optional.empty());

        //when
        handler.handle(event);

        //then
        verifyNoInteractions(entityManager);
    }

    @Test
    void handleShouldLogWarnWhenVariableNotFound() {
        //given
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "missing",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(variable);

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setStatus(ProcessInstanceStatus.RUNNING);
        // no variable added → getVariable("missing") returns empty

        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .thenReturn(Optional.of(processInstanceEntity));

        //when - should complete without throwing and without persisting anything
        handler.handle(event);

        //then
        verifyNoInteractions(entityManager);
    }

    @Test
    void handleShouldCatchExceptionAndNotPropagate() {
        //given
        when(featureToggle.isEnabled(QueryFeatureToggles.PROCESS_VARIABLE_HISTORY)).thenReturn(true);
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(variable);

        ProcessVariableEntity variableEntity = new ProcessVariableEntity();
        variableEntity.setName("var");

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setStatus(ProcessInstanceStatus.RUNNING);
        processInstanceEntity.getVariables().add(variableEntity);

        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .thenReturn(Optional.of(processInstanceEntity));
        doThrow(new RuntimeException("DB error")).when(entityManager).persist(any());

        //when - exception must be swallowed
        handler.handle(event);

        //then - remove was never reached
        verify(entityManager, never()).remove(any());
    }

    @Test
    void handleShouldRemoveVariableButSkipHistoryWhenVariableIsEphemeral() {
        //given
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(variable, true);

        ProcessVariableEntity variableEntity = new ProcessVariableEntity();
        variableEntity.setName("var");

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setStatus(ProcessInstanceStatus.RUNNING);
        processInstanceEntity.getVariables().add(variableEntity);

        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .thenReturn(Optional.of(processInstanceEntity));

        //when
        handler.handle(event);

        //then - variable removed but no history persisted
        verify(entityManager).remove(variableEntity);
        verify(entityManager, never()).persist(any());
    }

    @Test
    void handleShouldRemoveVariableButSkipHistoryWhenFeatureFlagDisabled() {
        //given
        when(featureToggle.isEnabled(QueryFeatureToggles.PROCESS_VARIABLE_HISTORY)).thenReturn(false);
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(
            "eventId",
            System.currentTimeMillis(),
            variable
        );

        ProcessVariableEntity variableEntity = new ProcessVariableEntity();
        variableEntity.setName("var");

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setStatus(ProcessInstanceStatus.RUNNING);
        processInstanceEntity.getVariables().add(variableEntity);

        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .thenReturn(Optional.of(processInstanceEntity));

        //when
        handler.handle(event);

        //then - variable removed but no history persisted
        verify(entityManager).remove(variableEntity);
        verify(entityManager, never()).persist(any());
    }
}
