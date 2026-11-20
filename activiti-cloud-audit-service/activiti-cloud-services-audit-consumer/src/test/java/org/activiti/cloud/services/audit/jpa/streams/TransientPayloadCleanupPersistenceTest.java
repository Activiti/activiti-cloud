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
package org.activiti.cloud.services.audit.jpa.streams;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.jpa.converters.EventContextInfoAppender;
import org.activiti.cloud.services.audit.jpa.converters.VariableCreatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableDeletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableUpdatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.json.VariableJpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationErrorReceivedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationRequestSentEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationResultReceivedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCancelledAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCreatedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableCreatedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableDeletedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableUpdatedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableValue;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransientPayloadCleanupPersistenceTest {

    private LocalContainerEntityManagerFactoryBean factory;
    private EntityManager entityManager;
    private HikariDataSource dataSource;

    private final APIEventToEntityConverters eventConverters = new APIEventToEntityConverters(
        Set.of(
            new VariableCreatedEventConverter(new EventContextInfoAppender()),
            new VariableUpdatedEventConverter(new EventContextInfoAppender()),
            new VariableDeletedEventConverter(new EventContextInfoAppender())
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

    private void verifyOnlyTerminatingProcessPayloadsAreCleared(String state, String relationship) {
        var payloads = seed("target", "app");
        var otherProcess = seed("other", "app");
        var otherApplication = seed("target", "other-app");
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
        var cleanup = new TransientAuditPayloadCleanup(entityManager);

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
        }
        for (AuditEventEntity event : List.of(otherProcess.getFirst(), otherApplication.getFirst())) {
            assertThat(
                (Object) entityManager
                    .find(VariableCreatedEventEntity.class, event.getId())
                    .getVariableInstance()
                    .getValue()
            ).isEqualTo("secret");
        }
        assertThat(entityManager.find(terminal.getClass(), terminal.getId())).isNotNull();
        cleanup.cleanup(List.of(terminal));
    }

    @Test
    void should_preservePayloadsForNonTerminalEvents() {
        var payload = seed("running", "app").getFirst();
        var running = new ProcessCreatedAuditEventEntity();
        running.setProcessInstanceId("running");
        running.setAppName("app");

        new TransientAuditPayloadCleanup(entityManager).cleanup(List.of(running));
        entityManager.clear();

        assertThat(
            (Object) entityManager
                .find(VariableCreatedEventEntity.class, payload.getId())
                .getVariableInstance()
                .getValue()
        ).isEqualTo("secret");
    }

    @Test
    void should_rollbackRedactionWithTransaction() {
        var payload = seed("rollback", "app").getFirst();
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
        var terminal = new ProcessCompletedEventEntity();
        terminal.setProcessInstanceId("rollback");
        terminal.setAppName("app");

        new TransientAuditPayloadCleanup(entityManager).cleanup(List.of(terminal));
        entityManager.getTransaction().rollback();
        entityManager.clear();
        entityManager.getTransaction().begin();

        assertThat(
            (Object) entityManager
                .find(VariableCreatedEventEntity.class, payload.getId())
                .getVariableInstance()
                .getValue()
        ).isEqualTo("secret");
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
        var metadata = new ProcessCreatedAuditEventEntity();
        List<AuditEventEntity> events = List.of(created, updated, deleted, requested, result, error, metadata);
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
}
