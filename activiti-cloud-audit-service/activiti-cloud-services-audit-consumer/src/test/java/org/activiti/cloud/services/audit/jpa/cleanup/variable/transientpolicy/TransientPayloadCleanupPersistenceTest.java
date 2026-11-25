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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.process.model.BPMNMessage;
import org.activiti.api.process.model.BPMNSignal;
import org.activiti.api.process.model.events.BPMNMessageEvent;
import org.activiti.api.process.model.events.BPMNSignalEvent;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.runtime.model.impl.BPMNMessageImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.AuditPayloadCleanup;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy.payload.AuditPayloadBatchWriter;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy.payload.AuditPayloadUpdateStrategy;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy.payload.VariablePayloadUpdateStrategy;
import org.activiti.cloud.services.audit.jpa.converters.EventContextInfoAppender;
import org.activiti.cloud.services.audit.jpa.converters.MessageReceivedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.MessageSentEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.MessageWaitingEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.SignalReceivedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableCreatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableDeletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableUpdatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.json.JpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.converters.json.MessageJpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.converters.json.SignalJpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.converters.json.VariableJpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationErrorReceivedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationRequestSentEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationResultReceivedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageReceivedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageSentAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageWaitingAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCancelledAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCreatedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.SignalReceivedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableCreatedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableDeletedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariablePayloadEvent;
import org.activiti.cloud.services.audit.jpa.events.VariableUpdatedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableValue;
import org.activiti.cloud.services.audit.jpa.streams.config.AuditJPAStreamsAutoConfiguration;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransientPayloadCleanupPersistenceTest {

    private LocalContainerEntityManagerFactoryBean factory;
    private EntityManager entityManager;
    private HikariDataSource dataSource;
    private AuditPayloadCleanup cleanup;
    private AuditPayloadBatchWriter payloadBatchWriter;

    private final APIEventToEntityConverters eventConverters = new APIEventToEntityConverters(
        Set.of(
            new VariableCreatedEventConverter(new EventContextInfoAppender()),
            new VariableUpdatedEventConverter(new EventContextInfoAppender()),
            new VariableDeletedEventConverter(new EventContextInfoAppender()),
            new MessageWaitingEventConverter(new EventContextInfoAppender()),
            new MessageSentEventConverter(new EventContextInfoAppender()),
            new MessageReceivedEventConverter(new EventContextInfoAppender()),
            new SignalReceivedEventConverter(new EventContextInfoAppender())
        )
    );

    @BeforeAll
    void createDatabase() {
        factory = new LocalContainerEntityManagerFactoryBean();
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("org.activiti.cloud.services.audit.jpa.events");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(
            Map.of(
                "hibernate.hbm2ddl.auto",
                "create-drop",
                "hibernate.physical_naming_strategy",
                PhysicalNamingStrategySnakeCaseImpl.class.getName(),
                "hibernate.hbm2ddl.halt_on_error",
                "true"
            )
        );
        factory.afterPropertiesSet();
    }

    @BeforeEach
    void beginTransaction() {
        entityManager = factory.getObject().createEntityManager();
        entityManager.getTransaction().begin();
        var configuration = new AuditJPAStreamsAutoConfiguration();
        payloadBatchWriter = configuration.auditPayloadBatchWriter(entityManager);
        cleanup = configuration.auditPayloadCleanup(entityManager, payloadBatchWriter, 100);
    }

    @AfterEach
    void rollbackTransaction() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    @AfterAll
    void closeDatabase() {
        factory.destroy();
        dataSource.close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "root", "child", "linked" })
    void should_clearOnlyCompletedProcessPayloads(String relationship) {
        verifyOnlyTerminatingProcessPayloadsAreCleared("completed", relationship);
    }

    @ParameterizedTest
    @ValueSource(strings = { "root", "child", "linked" })
    void should_clearOnlyCancelledProcessPayloads(String relationship) {
        verifyOnlyTerminatingProcessPayloadsAreCleared("cancelled", relationship);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "app" })
    void should_clearPayloadsRegardlessOfApplicationName(String appName) {
        for (ProcessAuditEventEntity terminal : List.of(
            new ProcessCompletedEventEntity(),
            new ProcessCancelledAuditEventEntity()
        )) {
            String processId = UUID.randomUUID().toString();
            var payloads = seed(processId, appName);
            terminal.setProcessInstanceId(processId);
            terminal.setAppName(appName);
            entityManager.persist(terminal);

            cleanup.cleanup(List.of(terminal));

            entityManager.clear();

            for (AuditEventEntity event : payloads) {
                var stored = entityManager.find(event.getClass(), event.getId());
                assertThat(stored.getAppName()).isEqualTo(appName);
                assertThat(stored.getProcessInstanceId()).isEqualTo(processId);
                assertThat(stored.getEventId()).isEqualTo(event.getEventId());
                assertThat(stored.getTimestamp()).isEqualTo(123L);
                if (stored instanceof VariablePayloadEvent payload) {
                    assertThat(payload.isVariablePayloadEmpty()).isTrue();
                }
            }
        }
    }

    @Test
    void should_preservePayloadsForNonTerminalEvents() {
        var payloads = seed("running", "app");
        var payload = payloads.getFirst();
        var running = new ProcessCreatedAuditEventEntity();
        running.setProcessInstanceId("running");
        running.setAppName("app");

        cleanup.cleanup(List.of(running));
        entityManager.clear();

        assertThat(
            (Object) entityManager
                .find(VariableCreatedEventEntity.class, payload.getId())
                .getVariableInstance()
                .getValue()
        ).isEqualTo("secret");
        assertMessageAndSignalPayloadsRetained(payloads);
    }

    @Test
    void should_rollbackRedactionWithTransaction() {
        var payloads = seed("rollback", "app");
        var payload = payloads.getFirst();
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
        var terminal = new ProcessCompletedEventEntity();
        terminal.setProcessInstanceId("rollback");
        terminal.setAppName("app");

        cleanup.cleanup(List.of(terminal));
        entityManager.getTransaction().rollback();
        entityManager.clear();
        entityManager.getTransaction().begin();

        assertThat(
            (Object) entityManager
                .find(VariableCreatedEventEntity.class, payload.getId())
                .getVariableInstance()
                .getValue()
        ).isEqualTo("secret");
        assertMessageAndSignalPayloadsRetained(payloads);
    }

    @ParameterizedTest
    @ValueSource(ints = { 10, 11, 21 })
    void should_persistAllPayloadFamiliesAcrossPageBoundaries(int histories) {
        List<AuditEventEntity> events = IntStream.range(0, histories)
            .mapToObj(_ -> seed("paged", "app"))
            .flatMap(List::stream)
            .toList();
        var otherProcess = seed("other-paged", "app");
        var otherApplication = seed("other-app-paged", "other-app");
        var terminal = new ProcessCompletedEventEntity();
        terminal.setProcessInstanceId("paged");
        terminal.setAppName("app");

        cleanup.cleanup(List.of(terminal));

        entityManager.clear();

        for (AuditEventEntity event : events) {
            var stored = entityManager.find(event.getClass(), event.getId());
            assertThat(stored.getEventId()).isEqualTo(event.getEventId());
            assertThat(stored.getTimestamp()).isEqualTo(123L);
            assertThat(stored.getProcessInstanceId()).isEqualTo("paged");
            assertThat(stored.getAppName()).isEqualTo("app");
            if (stored instanceof VariablePayloadEvent payload) {
                assertThat(payload.isVariablePayloadEmpty()).isTrue();
            }
            if (stored instanceof VariableAuditEventEntity variable) {
                assertThat(variable.getVariableInstance().getName()).isEqualTo("payload");
                assertThat(variable.getVariableInstance().getProcessInstanceId()).isEqualTo("paged");
            }
            if (stored instanceof IntegrationEventEntity integration) {
                assertThat(integration.getIntegrationContext().getId()).isEqualTo("context-paged");
            }
            if (stored instanceof MessageAuditEventEntity message) {
                assertMessageMetadata(message.getMessage(), "paged");
            }
            if (stored instanceof SignalReceivedAuditEventEntity signal) {
                assertSignalMetadata(signal.getSignal(), "paged");
            }
        }
        for (List<AuditEventEntity> isolated : List.of(otherProcess, otherApplication)) {
            assertThat(
                (Object) entityManager
                    .find(VariableCreatedEventEntity.class, isolated.getFirst().getId())
                    .getVariableInstance()
                    .getValue()
            ).isEqualTo("secret");
            assertMessageAndSignalPayloadsRetained(isolated);
        }
    }

    @Test
    void should_rollbackEarlierPageWrites_whenLaterBatchFails() {
        var events = IntStream.range(0, 101)
            .mapToObj(index -> {
                var event = new VariableCreatedEventEntity();
                event.setVariableInstance(
                    new VariableInstanceImpl<>("payload", "string", "secret", "failed-batch", null)
                );
                event.setProcessInstanceId("failed-batch");
                event.setAppName("app");
                entityManager.persist(event);
                return event;
            })
            .toList();
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
        var delegate = new VariablePayloadUpdateStrategy(payloadBatchWriter);
        var failingStrategy = mock(AuditPayloadUpdateStrategy.class);
        var batches = new AtomicInteger();
        given(failingStrategy.supports(any())).willReturn(true);
        willAnswer(invocation -> {
            if (batches.incrementAndGet() == 2) {
                throw new IllegalStateException("later batch failed");
            }
            delegate.update(invocation.getArgument(0));
            return null;
        })
            .given(failingStrategy)
            .update(any());
        var failingCleanup = new TransientAuditPayloadCleanup(entityManager, List.of(failingStrategy), 100);
        var terminal = new ProcessCompletedEventEntity();
        terminal.setProcessInstanceId("failed-batch");
        terminal.setAppName("app");

        List<AuditEventEntity> terminalEvents = List.of(terminal);
        assertThatThrownBy(() -> failingCleanup.cleanup(terminalEvents))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("later batch failed");
        assertThat(batches.get()).isEqualTo(2);
        entityManager.clear();
        assertThat(
            (Object) entityManager
                .find(VariableCreatedEventEntity.class, events.getFirst().getId())
                .getVariableInstance()
                .getValue()
        ).isNull();
        assertThat(
            (Object) entityManager
                .find(VariableCreatedEventEntity.class, events.getLast().getId())
                .getVariableInstance()
                .getValue()
        ).isEqualTo("secret");
        entityManager.getTransaction().rollback();
        entityManager.clear();
        entityManager.getTransaction().begin();

        for (var event : events) {
            assertThat(
                (Object) entityManager
                    .find(VariableCreatedEventEntity.class, event.getId())
                    .getVariableInstance()
                    .getValue()
            ).isEqualTo("secret");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "variable", "integration", "message", "signal" })
    void should_redactRestoredPayloadWithTransaction(String family) {
        String processId = "refreshed-" + family;
        var events = seed(processId, "app");
        AuditEventEntity event = switch (family) {
            case "variable" -> events
                .stream()
                .filter(VariableCreatedEventEntity.class::isInstance)
                .findFirst()
                .orElseThrow();
            case "integration" -> events
                .stream()
                .filter(IntegrationRequestSentEventEntity.class::isInstance)
                .findFirst()
                .orElseThrow();
            case "message" -> events
                .stream()
                .filter(MessageSentAuditEventEntity.class::isInstance)
                .findFirst()
                .orElseThrow();
            case "signal" -> events
                .stream()
                .filter(SignalReceivedAuditEventEntity.class::isInstance)
                .findFirst()
                .orElseThrow();
            default -> throw new IllegalArgumentException(family);
        };
        String column = switch (family) {
            case "variable" -> "variable_instance";
            case "integration" -> "integration_context";
            case "message" -> "message";
            case "signal" -> "signal";
            default -> throw new IllegalArgumentException(family);
        };
        String original = (String) entityManager
            .createNativeQuery("select " + column + " from audit_event where id = :id")
            .setParameter("id", event.getId())
            .getSingleResult();
        var terminal = new ProcessCompletedEventEntity();
        terminal.setProcessInstanceId(processId);
        terminal.setAppName("app");
        cleanup.cleanup(List.of(terminal));
        entityManager
            .createNativeQuery("update audit_event set " + column + " = :payload where id = :id")
            .setParameter("payload", original)
            .setParameter("id", event.getId())
            .executeUpdate();
        entityManager.clear();
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();

        cleanup.cleanup(List.of(terminal));

        entityManager.clear();

        var stored = entityManager.find(event.getClass(), event.getId());
        assertThat(((VariablePayloadEvent) stored).isVariablePayloadEmpty()).isTrue();
        entityManager.getTransaction().rollback();
        entityManager.clear();
        entityManager.getTransaction().begin();

        var restored = entityManager.find(event.getClass(), event.getId());
        assertThat(((VariablePayloadEvent) restored).isVariablePayloadEmpty()).isFalse();
    }

    private void verifyOnlyTerminatingProcessPayloadsAreCleared(String state, String relationship) {
        var payloads = seed("target", "app");
        var otherProcess = seed("other", "app");
        var otherApplication = seed("other-app-target", "other-app");
        ProcessAuditEventEntity terminal = state.equals("completed")
            ? new ProcessCompletedEventEntity()
            : new ProcessCancelledAuditEventEntity();
        var process = new ProcessInstanceImpl();
        process.setId("target");
        process.setParentId(relationship.equals("child") ? "parent" : null);
        process.setRootProcessInstanceId(relationship.equals("child") ? "parent" : "target");
        terminal.setProcessInstance(process);
        terminal.setProcessInstanceId("target");
        terminal.setParentProcessInstanceId(relationship.equals("child") ? "parent" : null);
        terminal.setAppName("app");
        entityManager.persist(terminal);

        cleanup.cleanup(List.of(terminal, terminal));

        entityManager.flush();
        entityManager.clear();

        for (AuditEventEntity event : payloads) {
            var stored = entityManager.find(event.getClass(), event.getId());
            assertThat(stored.getTimestamp()).isEqualTo(123L);
            assertThat(stored.getProcessInstanceId()).isEqualTo("target");
            if (stored instanceof VariableAuditEventEntity variable) {
                var instance = variable.getVariableInstance();
                assertThat(instance).isNotNull();
                assertThat(instance.getName()).isEqualTo("payload");
                assertThat(instance.getType()).isEqualTo("string");
                assertThat(instance.getProcessInstanceId()).isEqualTo("target");
                assertThat(instance.getTaskId()).isNull();
                assertThat((Object) instance.getValue()).isNull();
                var apiEvent = (CloudVariableEvent) eventConverters
                    .getConverterByEventTypeName(variable.getEventType())
                    .convertToAPI(variable);
                assertThat(apiEvent.getId()).isEqualTo(variable.getEventId());
                assertThat(apiEvent.getTimestamp()).isEqualTo(123L);
                assertThat(apiEvent.getAppName()).isEqualTo("app");
                assertThat(apiEvent.getProcessInstanceId()).isEqualTo("target");
                assertThat(apiEvent.getEntity()).usingRecursiveComparison().isEqualTo(instance);
                if (apiEvent instanceof CloudVariableUpdatedEvent updatedEvent) {
                    assertThat((Object) updatedEvent.getPreviousValue()).isNull();
                }
                var storedPayloads = (Object[]) entityManager
                    .createNativeQuery(
                        """
                        select variable_instance, variable_previous_value from audit_event
                        where id = :id
                        """
                    )
                    .setParameter("id", variable.getId())
                    .getSingleResult();
                assertThat(storedPayloads[0]).isInstanceOf(String.class);
                String storedJson = (String) storedPayloads[0];
                assertThat(storedJson).isNotBlank().doesNotContain("secret");
                assertThat(new VariableJpaJsonConverter().convertToEntityAttribute(storedJson))
                    .usingRecursiveComparison()
                    .isEqualTo(instance);
                assertThat(storedPayloads[1]).isNull();
            }
            if (stored instanceof VariableUpdatedEventEntity updated) {
                assertThat(updated.getPreviousValue()).isNull();
            }
            if (stored instanceof IntegrationEventEntity integration) {
                assertThat(integration.getIntegrationContext().getInBoundVariables()).isEmpty();
                assertThat(integration.getIntegrationContext().getOutBoundVariables()).isEmpty();
                assertThat(integration.getIntegrationContext().getId()).startsWith("context-");
            }
            if (stored instanceof MessageAuditEventEntity messageEvent) {
                var message = messageEvent.getMessage();
                assertMessageMetadata(message, "target");
                assertThat(message.getMessagePayload().getVariables()).isEmpty();
                assertThat(message.getMessagePayload().getId()).isEqualTo(
                    ((MessageAuditEventEntity) event).getMessage().getMessagePayload().getId()
                );
                var apiEvent = (CloudBPMNMessageEvent) eventConverters
                    .getConverterByEventTypeName(stored.getEventType())
                    .convertToAPI(stored);
                assertThat(apiEvent.getEntity()).usingRecursiveComparison().isEqualTo(message);
                assertStoredJson(stored, "message", new MessageJpaJsonConverter(), message);
            }
            if (stored instanceof SignalReceivedAuditEventEntity signalEvent) {
                var signal = signalEvent.getSignal();
                assertSignalMetadata(signal, "target");
                assertThat(signal.getSignalPayload().getVariables()).isEmpty();
                assertThat(signal.getSignalPayload().getId()).isEqualTo(
                    ((SignalReceivedAuditEventEntity) event).getSignal().getSignalPayload().getId()
                );
                var apiEvent = (CloudBPMNSignalReceivedEvent) eventConverters
                    .getConverterByEventTypeName(stored.getEventType())
                    .convertToAPI(stored);
                assertThat(apiEvent.getEntity()).usingRecursiveComparison().isEqualTo(signal);
                assertStoredJson(stored, "signal", new SignalJpaJsonConverter(), signal);
            }
        }
        for (AuditEventEntity event : List.of(otherProcess.getFirst(), otherApplication.getFirst())) {
            assertThat(
                (Object) entityManager
                    .find(VariableCreatedEventEntity.class, event.getId())
                    .getVariableInstance()
                    .getValue()
            ).isEqualTo("secret");
        }
        assertMessageAndSignalPayloadsRetained(otherProcess);
        assertMessageAndSignalPayloadsRetained(otherApplication);

        cleanup.cleanup(List.of(terminal));
        entityManager.clear();
        for (AuditEventEntity event : payloads) {
            var stored = entityManager.find(event.getClass(), event.getId());
            if (stored instanceof VariablePayloadEvent payload) {
                assertThat(payload.isVariablePayloadEmpty()).isTrue();
            }
        }
    }

    private List<AuditEventEntity> seed(String processId, String appName) {
        var variable = new VariableInstanceImpl<>("payload", "string", "secret", processId, null);
        var created = new VariableCreatedEventEntity();
        created.setVariableInstance(variable);
        created.setEventType(VariableEvent.VariableEvents.VARIABLE_CREATED.name());
        var updated = new VariableUpdatedEventEntity();
        updated.setVariableInstance(variable);
        updated.setEventType(VariableEvent.VariableEvents.VARIABLE_UPDATED.name());
        updated.setPreviousValue(new VariableValue<>("previous secret"));
        var deleted = new VariableDeletedEventEntity();
        deleted.setVariableInstance(variable);
        deleted.setEventType(VariableEvent.VariableEvents.VARIABLE_DELETED.name());
        var context = new IntegrationContextImpl();
        context.setId("context-" + processId);
        context.setProcessInstanceId(processId);
        context.getInBoundVariables().put("input", "secret");
        context.getOutBoundVariables().put("output", "secret");
        var requestedEvent = new CloudIntegrationRequestedEventImpl(context);
        requestedEvent.setSequenceNumber(0);
        var resultEvent = new CloudIntegrationResultReceivedEventImpl(context);
        resultEvent.setSequenceNumber(0);
        var errorEvent = new CloudIntegrationErrorReceivedEventImpl(
            context,
            "ERROR",
            "execution error",
            "ErrorClass",
            List.of()
        );
        errorEvent.setSequenceNumber(0);
        var requested = new IntegrationRequestSentEventEntity(requestedEvent);
        var result = new IntegrationResultReceivedEventEntity(resultEvent);
        var error = new IntegrationErrorReceivedEventEntity(errorEvent);
        var waiting = new MessageWaitingAuditEventEntity();
        waiting.setMessage(message(processId, "waiting"));
        waiting.setEntityId("waiting-activity");
        waiting.setProcessDefinitionId("definition");
        waiting.setEventType(BPMNMessageEvent.MessageEvents.MESSAGE_WAITING.name());
        var sent = new MessageSentAuditEventEntity();
        sent.setMessage(message(processId, "sent"));
        sent.setEntityId("sent-activity");
        sent.setProcessDefinitionId("definition");
        sent.setEventType(BPMNMessageEvent.MessageEvents.MESSAGE_SENT.name());
        var received = new MessageReceivedAuditEventEntity();
        received.setMessage(message(processId, "received"));
        received.setEntityId("received-activity");
        received.setProcessDefinitionId("definition");
        received.setEventType(BPMNMessageEvent.MessageEvents.MESSAGE_RECEIVED.name());
        var signal = new BPMNSignalImpl("signal-activity");
        signal.setProcessInstanceId(processId);
        signal.setProcessDefinitionId("definition");
        signal.setSignalPayload(new SignalPayload("notification-signal", Map.of("signal-variable", "secret")));
        var signalled = new SignalReceivedAuditEventEntity();
        signalled.setSignal(signal);
        signalled.setEntityId("signal-activity");
        signalled.setProcessDefinitionId("definition");
        signalled.setEventType(BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED.name());
        var metadata = new ProcessCreatedAuditEventEntity();
        List<AuditEventEntity> events = List.of(
            created,
            updated,
            deleted,
            requested,
            result,
            error,
            waiting,
            sent,
            received,
            signalled,
            metadata
        );
        for (AuditEventEntity event : events) {
            event.setProcessInstanceId(processId);
            event.setAppName(appName);
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(123L);
            entityManager.persist(event);
        }
        entityManager.flush();
        return events;
    }

    private BPMNMessageImpl message(String processId, String name) {
        var message = new BPMNMessageImpl(name + "-activity");
        message.setProcessInstanceId(processId);
        message.setProcessDefinitionId("definition");
        message.setMessagePayload(
            new MessageEventPayload(
                "notification",
                "correlation",
                "message-business",
                Map.of("message-variable", "secret")
            )
        );
        return message;
    }

    private void assertMessageMetadata(BPMNMessage message, String processId) {
        assertThat(message.getProcessInstanceId()).isEqualTo(processId);
        assertThat(message.getProcessDefinitionId()).isEqualTo("definition");
        assertThat(message.getElementId()).endsWith("-activity");
        assertThat(message.getMessagePayload().getName()).isEqualTo("notification");
        assertThat(message.getMessagePayload().getCorrelationKey()).isEqualTo("correlation");
        assertThat(message.getMessagePayload().getBusinessKey()).isEqualTo("message-business");
    }

    private void assertSignalMetadata(BPMNSignal signal, String processId) {
        assertThat(signal.getProcessInstanceId()).isEqualTo(processId);
        assertThat(signal.getProcessDefinitionId()).isEqualTo("definition");
        assertThat(signal.getElementId()).isEqualTo("signal-activity");
        assertThat(signal.getSignalPayload().getName()).isEqualTo("notification-signal");
    }

    private void assertStoredJson(
        AuditEventEntity event,
        String column,
        JpaJsonConverter<?> converter,
        Object expected
    ) {
        String json = (String) entityManager
            .createNativeQuery("select " + column + " from audit_event where id = :id")
            .setParameter("id", event.getId())
            .getSingleResult();
        assertThat(json).isNotBlank().doesNotContain("secret");
        Object payload = converter.convertToEntityAttribute(json);
        assertThat(payload).usingRecursiveComparison().isEqualTo(expected);
    }

    private void assertMessageAndSignalPayloadsRetained(List<AuditEventEntity> events) {
        for (AuditEventEntity event : events) {
            var stored = entityManager.find(event.getClass(), event.getId());
            if (stored instanceof MessageAuditEventEntity message) {
                assertThat(message.getMessage().getMessagePayload().getVariables()).containsExactlyEntriesOf(
                    Map.of("message-variable", "secret")
                );
            }
            if (stored instanceof SignalReceivedAuditEventEntity signal) {
                assertThat(signal.getSignal().getSignalPayload().getVariables()).containsExactlyEntriesOf(
                    Map.of("signal-variable", "secret")
                );
            }
        }
    }
}
