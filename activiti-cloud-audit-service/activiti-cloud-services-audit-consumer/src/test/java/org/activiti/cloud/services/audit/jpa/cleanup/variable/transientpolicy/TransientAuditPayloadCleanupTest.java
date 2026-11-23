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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.stream.IntStream;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy.payload.AuditPayloadUpdateStrategy;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationRequestSentEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageSentAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.SignalReceivedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableCreatedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableUpdatedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
        cleanup = new TransientAuditPayloadCleanup(entityManager, List.of(unrelatedStrategy, matchingStrategy), 100);
        terminal = new ProcessCompletedEventEntity();
        terminal.setProcessInstanceId("process");
        terminal.setAppName("app");
        given(entityManager.createQuery(anyString(), eq(AuditEventEntity.class))).willReturn(query);
        given(query.setParameter(anyString(), any())).willReturn(query);
        given(query.setMaxResults(anyInt())).willReturn(query);
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
        willAnswer(_ -> {
            assertThat(event.isVariablePayloadEmpty()).isTrue();
            assertThat(event.getPreviousValue()).isNull();
            assertThat((Object) event.getVariableInstance().getValue()).isNull();
            return null;
        })
            .given(matchingStrategy)
            .update(List.of(event));

        cleanup.cleanup(List.of(terminal, terminal));

        var order = inOrder(entityManager, matchingStrategy);
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
        order.verify(entityManager).createQuery(anyString(), eq(AuditEventEntity.class));
        order.verify(matchingStrategy).update(List.of(event));
        then(unrelatedStrategy).should(never()).update(any());
        then(entityManager).should().createQuery(anyString(), eq(AuditEventEntity.class));
    }

    @Test
    void should_failBeforeClearing_whenNoStrategySupportsEvent() {
        var event = variable("current");
        given(query.getResultList()).willReturn(List.of(event));

        List<AuditEventEntity> terminalEvents = List.of(terminal);
        assertThatThrownBy(() -> cleanup.cleanup(terminalEvents))
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
        willThrow(new IllegalStateException("write failed")).given(matchingStrategy).update(List.of(event));

        List<AuditEventEntity> terminalEvents = List.of(terminal);
        assertThatThrownBy(() -> cleanup.cleanup(terminalEvents))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("write failed");
    }

    @Test
    void should_skipFurtherWrites_whenPayloadWasAlreadyCleared() {
        var event = variable("current");
        given(query.getResultList()).willReturn(List.of(event));
        given(matchingStrategy.supports(event)).willReturn(true);

        cleanup.cleanup(List.of(terminal));
        cleanup.cleanup(List.of(terminal));

        then(matchingStrategy).should().update(List.of(event));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 99, 100, 101, 200, 205 })
    void should_boundPagesAndAdvanceCursorWithoutOffsets(int eventCount) {
        verifyPagination(eventCount, 100);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 7, 37 })
    void should_useConfiguredBatchSizeForLimitsAndContinuation(int batchSize) {
        verifyPagination(batchSize * 2 + 1, batchSize);
    }

    private void verifyPagination(int eventCount, int batchSize) {
        cleanup = new TransientAuditPayloadCleanup(
            entityManager,
            List.of(unrelatedStrategy, matchingStrategy),
            batchSize
        );
        List<AuditEventEntity> events = IntStream.range(0, eventCount)
            .mapToObj(index -> {
                var event = variable("current");
                event.setId(10L + index * 2);
                return (AuditEventEntity) event;
            })
            .toList();
        int pageCount = eventCount / batchSize + 1;
        var pages = IntStream.range(0, pageCount)
            .mapToObj(index -> events.subList(index * batchSize, Math.min((index + 1) * batchSize, eventCount)))
            .toList();
        var pageIterator = pages.iterator();
        given(query.getResultList()).willAnswer(call -> pageIterator.next());
        if (eventCount > 0) {
            given(matchingStrategy.supports(any())).willReturn(true);
        }

        cleanup.cleanup(List.of(terminal));

        var order = inOrder(entityManager, query, matchingStrategy);
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
        for (List<AuditEventEntity> page : pages) {
            order.verify(entityManager).createQuery(anyString(), eq(AuditEventEntity.class));
            order.verify(query).setMaxResults(batchSize);
            order.verify(query).getResultList();
            if (!page.isEmpty()) {
                order.verify(matchingStrategy).update(page);
            }
            order.verify(entityManager).clear();
        }
        var queries = ArgumentCaptor.forClass(String.class);
        then(entityManager).should(times(pageCount)).createQuery(queries.capture(), eq(AuditEventEntity.class));
        assertThat(queries.getAllValues().getFirst()).doesNotContain(":lastId");
        assertThat(queries.getAllValues()).allSatisfy(sql ->
            assertThat(sql)
                .contains("order by e.id", "e.processInstanceId = :processInstanceId")
                .doesNotContain("appName")
        );
        for (int index = 1; index < pageCount; index++) {
            assertThat(queries.getAllValues().get(index)).contains("e.id > :lastId");
            then(query)
                .should()
                .setParameter("lastId", events.get(index * batchSize - 1).getId());
        }
        if (pageCount == 1) {
            then(query).should(never()).setParameter(eq("lastId"), any());
        }
        then(query).should(never()).setParameter(eq("appName"), any());
        then(query).should(times(pageCount)).setParameter("processInstanceId", "process");
        then(query).should(never()).setFirstResult(anyInt());
        then(unrelatedStrategy).should(never()).update(any());
    }

    @Test
    void should_advancePastFullPageWithoutPayloads() {
        List<AuditEventEntity> emptyPage = IntStream.rangeClosed(1, 100)
            .mapToObj(index -> {
                var event = new VariableCreatedEventEntity();
                event.setId((long) index);
                return (AuditEventEntity) event;
            })
            .toList();
        var event = variable("current");
        event.setId(101L);
        given(query.getResultList()).willReturn(emptyPage, List.of(event));
        given(matchingStrategy.supports(event)).willReturn(true);

        cleanup.cleanup(List.of(terminal));

        then(query).should().setParameter("lastId", 100L);
        then(query).should(times(2)).getResultList();
        then(matchingStrategy).should().supports(event);
        then(matchingStrategy).should().update(List.of(event));
        then(matchingStrategy).shouldHaveNoMoreInteractions();
        then(unrelatedStrategy).should(never()).update(any());
    }

    @Test
    void should_groupEventsBySupportingStrategy() {
        var first = variable("current");
        var second = variable("previous");
        var integration = emptyIntegration();
        integration.getIntegrationContext().getInBoundVariables().put("input", "secret");
        given(query.getResultList()).willReturn(List.of(first, integration, second));
        given(matchingStrategy.supports(any())).willReturn(true);
        given(unrelatedStrategy.supports(any())).willAnswer(invocation -> invocation.getArgument(0) == integration);

        cleanup.cleanup(List.of(terminal));

        then(matchingStrategy).should().update(List.of(first, second));
        then(unrelatedStrategy).should().update(List.of(integration));
        assertThat(first.isVariablePayloadEmpty()).isTrue();
        assertThat(second.isVariablePayloadEmpty()).isTrue();
        assertThat(integration.isVariablePayloadEmpty()).isTrue();
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
