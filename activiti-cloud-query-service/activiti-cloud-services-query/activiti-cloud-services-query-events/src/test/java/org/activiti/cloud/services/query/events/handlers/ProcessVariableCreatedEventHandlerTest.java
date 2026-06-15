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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.Set;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessVariableCreatedEventHandlerTest {

    @InjectMocks
    private ProcessVariableCreatedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    @Mock
    private FeatureToggle featureToggle;

    @Test
    void handleShouldPersistVariableAndHistoryEntry() {
        //given
        when(featureToggle.isEnabled(QueryFeatureToggles.PROCESS_VARIABLE_HISTORY)).thenReturn(true);
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(
            "eventId",
            System.currentTimeMillis(),
            variable
        );
        event.setMessageId("msg-003");
        event.setSequenceNumber(7);

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .thenReturn(Optional.of(processInstanceEntity));
        when(entityManagerFinder.findTasksWithProcessVariables("procInstId")).thenReturn(Set.of());

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(entityManager, org.mockito.Mockito.times(2)).persist(captor.capture());

        var persisted = captor.getAllValues();
        var history = persisted
            .stream()
            .filter(ProcessVariableHistoryEntity.class::isInstance)
            .map(o -> (ProcessVariableHistoryEntity) o)
            .findFirst();
        assertThat(history).isPresent();
        assertThat(history.get().getProcessInstanceId()).isEqualTo("procInstId");
        assertThat(history.get().getVariableName()).isEqualTo("var");
        assertThat(history.get().isDeleted()).isFalse();
        assertThat(history.get().getMessageId()).isEqualTo("msg-003");
        assertThat(history.get().getSequenceNumber()).isEqualTo(7);
        assertThat(history.get().getEventTime()).isNotNull();
        assertThat(history.get().getRecordCreateTime()).isNotNull();
    }

    @Test
    void handleShouldPersistVariableButSkipHistoryWhenVariableIsEphemeral() {
        //given
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(variable, true);

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .thenReturn(Optional.of(processInstanceEntity));
        when(entityManagerFinder.findTasksWithProcessVariables("procInstId")).thenReturn(Set.of());

        //when
        handler.handle(event);

        //then - only variable persisted, no history
        verify(entityManager).persist(any(org.activiti.cloud.services.query.model.ProcessVariableEntity.class));
        verify(entityManager, never()).persist(any(ProcessVariableHistoryEntity.class));
    }

    @Test
    void handleShouldPersistVariableButSkipHistoryWhenFeatureFlagDisabled() {
        //given
        when(featureToggle.isEnabled(QueryFeatureToggles.PROCESS_VARIABLE_HISTORY)).thenReturn(false);
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>(
            "var",
            "string",
            "value",
            "procInstId",
            null
        );
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(
            "eventId",
            System.currentTimeMillis(),
            variable
        );

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .thenReturn(Optional.of(processInstanceEntity));
        when(entityManagerFinder.findTasksWithProcessVariables("procInstId")).thenReturn(Set.of());

        //when
        handler.handle(event);

        //then - only variable persisted, no history
        verify(entityManager).persist(any(org.activiti.cloud.services.query.model.ProcessVariableEntity.class));
        verify(entityManager, never()).persist(any(ProcessVariableHistoryEntity.class));
    }
}
