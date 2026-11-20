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
package org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy.payload.AuditPayloadUpdateStrategy;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationRequestSentEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageSentAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCreatedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.SignalReceivedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableCreatedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableUpdatedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransientAuditPayloadCleanupTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<AuditEventEntity> query;

    @Mock
    private AuditPayloadUpdateStrategy unrelatedStrategy;

    @Mock
    private AuditPayloadUpdateStrategy matchingStrategy;

    private TransientAuditPayloadCleanup cleanup;
    private ProcessCompletedEventEntity terminal;

    @BeforeEach
    void setUp() {
        cleanup = new TransientAuditPayloadCleanup(entityManager, List.of(unrelatedStrategy, matchingStrategy));
        terminal = new ProcessCompletedEventEntity();
        terminal.setProcessInstanceId("process");
        terminal.setAppName("app");
        given(entityManager.createQuery(anyString(), eq(AuditEventEntity.class))).willReturn(query);
        given(query.setParameter(anyString(), any())).willReturn(query);
    }

    @ParameterizedTest
    @ValueSource(strings = { "variable", "updated", "integration", "message", "signal" })
    void should_skipStrategies_whenPayloadIsEmpty(String type) {
        AuditEventEntity event = switch (type) {
            case "variable" -> new VariableCreatedEventEntity();
            case "updated" -> new VariableUpdatedEventEntity();
            case "integration" -> emptyIntegration();
            case "message" -> new MessageSentAuditEventEntity();
            case "signal" -> new SignalReceivedAuditEventEntity();
            default -> throw new IllegalArgumentException(type);
        };
        given(query.getResultList()).willReturn(List.of(event));

        cleanup.cleanup(List.of(terminal));

        verifyNoInteractions(unrelatedStrategy, matchingStrategy);
    }

    @ParameterizedTest
    @ValueSource(strings = { "current", "previous" })
    void should_clearPayloadBeforeCallingSupportingStrategy(String value) {
        var event = variable(value);
        given(query.getResultList()).willReturn(List.of(event));
        given(matchingStrategy.supports(event)).willReturn(true);
        given(matchingStrategy.update(event)).willAnswer(call -> {
            assertThat(event.isVariablePayloadEmpty()).isTrue();
            assertThat(event.getPreviousValue()).isNull();
            assertThat((Object) event.getVariableInstance().getValue()).isNull();
            return 1;
        });

        cleanup.cleanup(List.of(terminal, terminal));

        var order = inOrder(entityManager, matchingStrategy);
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
        order.verify(entityManager).createQuery(anyString(), eq(AuditEventEntity.class));
        order.verify(matchingStrategy).update(event);
        then(unrelatedStrategy).should(never()).update(any());
        then(entityManager).should().createQuery(anyString(), eq(AuditEventEntity.class));
    }

    @Test
    void should_failBeforeClearing_whenNoStrategySupportsEvent() {
        var event = variable("current");
        given(query.getResultList()).willReturn(List.of(event));

        assertThatThrownBy(() -> cleanup.cleanup(List.of(terminal)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No payload update strategy");

        assertThat((Object) event.getVariableInstance().getValue()).isEqualTo("secret");
        then(unrelatedStrategy).should(never()).update(any());
        then(matchingStrategy).should(never()).update(any());
    }

    @Test
    void should_propagateStrategyFailure() {
        var event = variable("current");
        given(query.getResultList()).willReturn(List.of(event));
        given(matchingStrategy.supports(event)).willReturn(true);
        given(matchingStrategy.update(event)).willThrow(new IllegalStateException("write failed"));

        assertThatThrownBy(() -> cleanup.cleanup(List.of(terminal)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("write failed");
    }

    @Test
    void should_skipFurtherWrites_whenPayloadWasAlreadyCleared() {
        var event = variable("current");
        given(query.getResultList()).willReturn(List.of(event));
        given(matchingStrategy.supports(event)).willReturn(true);
        given(matchingStrategy.update(event)).willReturn(1);

        cleanup.cleanup(List.of(terminal));
        cleanup.cleanup(List.of(terminal));

        then(matchingStrategy).should().update(event);
    }

    @Test
    void should_skipMetadataOnlyEvents() {
        given(query.getResultList()).willReturn(List.of(new ProcessCreatedAuditEventEntity()));

        cleanup.cleanup(List.of(terminal));

        verifyNoInteractions(unrelatedStrategy, matchingStrategy);
    }

    private IntegrationRequestSentEventEntity emptyIntegration() {
        var context = new IntegrationContextImpl();
        context.setId("context");
        var event = new CloudIntegrationRequestedEventImpl(context);
        event.setSequenceNumber(0);
        return new IntegrationRequestSentEventEntity(event);
    }

    private VariableUpdatedEventEntity variable(String value) {
        var event = new VariableUpdatedEventEntity();
        event.setProcessInstanceId("process");
        event.setVariableInstance(
            new VariableInstanceImpl<>("payload", "string", value.equals("current") ? "secret" : null, "process", null)
        );
        if (value.equals("previous")) {
            event.setPreviousValue(new VariableValue<>("previous secret"));
        }
        return event;
    }
}
