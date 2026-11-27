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
package org.activiti.cloud.services.query.events.handlers.cleanup.variable.transientpolicy;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.model.VariableValueJsonConverter;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import tools.jackson.databind.json.JsonMapper;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransientPayloadCleanupPersistenceTest {

    private LocalContainerEntityManagerFactoryBean factory;
    private EntityManager entityManager;

    @BeforeAll
    void createDatabase() {
        new VariableValueJsonConverter(JsonMapper.builder().build());
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(
            new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
            )
        );
        factory.setPackagesToScan("org.activiti.cloud.services.query.model");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(
            Map.of(
                "hibernate.hbm2ddl.auto",
                "create-drop",
                "hibernate.physical_naming_strategy",
                PhysicalNamingStrategySnakeCaseImpl.class.getName(),
                "hibernate.hbm2ddl.halt_on_error",
                "true",
                "hibernate.generate_statistics",
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
        seed("target");
        seed("parent");
        seed("child");
        seed("linked");
        var storedProcess = entityManager.find(ProcessInstanceEntity.class, "target");
        storedProcess.setParentId(relationship.equals("child") ? "parent" : null);
        storedProcess.setRootProcessInstanceId(relationship.equals("child") ? "parent" : "target");
        storedProcess.setLinkedProcessInstanceId(relationship.equals("linked") ? "parent" : null);
        var process = new ProcessInstanceImpl();
        process.setId("target");
        process.setRootProcessInstanceId(relationship.equals("child") ? "parent" : "target");
        process.setParentId(relationship.equals("child") ? "parent" : null);
        CloudRuntimeEvent<?, ?> terminal = state.equals("completed")
            ? new CloudProcessCompletedEventImpl(process)
            : new CloudProcessCancelledEventImpl(process);
        var cleanup = new TransientRuntimeVariableCleanup(entityManager);

        Instant started = Instant.now();
        cleanup.cleanup(List.of(terminal, terminal));
        Instant finished = Instant.now();
        assertCleanupTimestamp("target", started, finished);

        cleanup.cleanup(List.of(terminal));

        assertThat(count("ProcessVariable", "target")).isZero();
        assertThat(count("TaskVariable", "target")).isZero();
        assertThat(
            entityManager
                .createNativeQuery(
                    "select count(*) from task_process_variable where task_id = 'task-target'",
                    Long.class
                )
                .getSingleResult()
        ).isEqualTo(0L);
        var context = entityManager.find(IntegrationContextEntity.class, "integration-target");
        assertThat(context.getInBoundVariables()).isEmpty();
        assertThat(context.getOutBoundVariables()).isEmpty();
        assertThat(context.getErrorMessage()).isEqualTo("execution error");
        assertThat(entityManager.find(TaskEntity.class, "task-target")).isNotNull();
        for (String other : List.of("parent", "child", "linked")) {
            assertThat(entityManager.find(ProcessInstanceEntity.class, other).getLastVariableCleanup()).isNull();
            assertThat(count("ProcessVariable", other)).isEqualTo(1);
            assertThat(count("TaskVariable", other)).isEqualTo(1);
            assertThat(
                entityManager.find(IntegrationContextEntity.class, "integration-" + other).getInBoundVariables()
            ).containsEntry("input", "secret");
        }
    }

    @Test
    void should_preservePayloads_whenProcessIsStillRunning() {
        seed("running");
        var process = new ProcessInstanceImpl();
        process.setId("running");

        new TransientRuntimeVariableCleanup(entityManager).cleanup(List.of(new CloudProcessStartedEventImpl(process)));

        assertThat(entityManager.find(ProcessInstanceEntity.class, "running").getLastVariableCleanup()).isNull();
        assertThat(count("ProcessVariable", "running")).isEqualTo(1);
        assertThat(count("TaskVariable", "running")).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = { "completed", "cancelled" })
    void should_clearAllIntegrationContexts_withoutLoadingEntities(String state) {
        seed("target");
        seed("other");
        for (int i = 0; i < 110; i++) {
            var context = new IntegrationContextEntity();
            context.setId("bulk-" + i);
            context.setProcessInstanceId("target");
            context.setInBoundVariables(new HashMap<>(Map.of("input", "secret")));
            context.setOutBoundVariables(new HashMap<>(Map.of("output", "secret")));
            context.setErrorMessage("execution error");
            entityManager.persist(context);
        }
        var process = new ProcessInstanceImpl();
        process.setId("target");
        CloudRuntimeEvent<?, ?> terminal = state.equals("completed")
            ? new CloudProcessCompletedEventImpl(process)
            : new CloudProcessCancelledEventImpl(process);
        entityManager.flush();
        entityManager.clear();
        var statistics = factory.getObject().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        new TransientRuntimeVariableCleanup(entityManager).cleanup(List.of(terminal));

        assertThat(statistics.getEntityLoadCount()).isZero();
        assertThat(
            entityManager
                .createNativeQuery("select last_variable_cleanup from process_instance where id = 'target'")
                .getSingleResult()
        ).isNotNull();
        assertThat(
            entityManager
                .createNativeQuery(
                    """
                    select count(*) from integration_context
                    where process_instance_id = 'target'
                      and inbound_variables = '{}' and out_bound_variables = '{}'
                      and error_message = 'execution error'
                    """,
                    Long.class
                )
                .getSingleResult()
        ).isEqualTo(111L);
        var other = entityManager.find(IntegrationContextEntity.class, "integration-other");
        assertThat(other.getInBoundVariables()).containsEntry("input", "secret");
        assertThat(other.getOutBoundVariables()).containsEntry("output", "secret");
    }

    @ParameterizedTest
    @ValueSource(strings = { "sql-null", "json-null", "empty", "inbound", "outbound" })
    void should_normalizeIntegrationVariableMaps(String payloadState) {
        seed("target");
        String inbound = switch (payloadState) {
            case "sql-null", "outbound" -> null;
            case "json-null" -> "null";
            case "inbound" -> "{\"input\":\"secret\"}";
            default -> "{}";
        };
        String outbound = switch (payloadState) {
            case "sql-null", "inbound" -> null;
            case "json-null" -> "null";
            case "outbound" -> "{\"output\":\"secret\"}";
            default -> "{}";
        };
        entityManager
            .createNativeQuery(
                """
                update integration_context
                set inbound_variables = :inbound, out_bound_variables = :outbound
                where id = 'integration-target'
                """
            )
            .setParameter("inbound", inbound)
            .setParameter("outbound", outbound)
            .executeUpdate();
        entityManager.clear();
        var process = new ProcessInstanceImpl();
        process.setId("target");

        new TransientRuntimeVariableCleanup(entityManager).cleanup(
            List.of(new CloudProcessCompletedEventImpl(process))
        );

        var context = entityManager.find(IntegrationContextEntity.class, "integration-target");
        assertThat(context.getInBoundVariables()).isEmpty();
        assertThat(context.getOutBoundVariables()).isEmpty();
        assertThat(context.getErrorMessage()).isEqualTo("execution error");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_rollbackPayloadCleanupAndTimestampWithTransaction(boolean cleanedPreviously) {
        String processId = "rollback-" + cleanedPreviously;
        seed(processId);
        Instant previous = cleanedPreviously ? Instant.parse("2000-01-01T00:00:00Z") : null;
        if (previous != null) {
            entityManager
                .createNativeQuery("update process_instance set last_variable_cleanup = :previous where id = :id")
                .setParameter("previous", previous.atOffset(ZoneOffset.UTC))
                .setParameter("id", processId)
                .executeUpdate();
        }
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
        var process = new ProcessInstanceImpl();
        process.setId(processId);

        Instant started = Instant.now();
        new TransientRuntimeVariableCleanup(entityManager).cleanup(
            List.of(new CloudProcessCompletedEventImpl(process))
        );
        Instant finished = Instant.now();
        assertCleanupTimestamp(processId, started, finished);
        assertThat(count("ProcessVariable", processId)).isZero();
        assertThat(count("TaskVariable", processId)).isZero();
        entityManager.getTransaction().rollback();
        entityManager.clear();
        entityManager.getTransaction().begin();

        assertThat(entityManager.find(ProcessInstanceEntity.class, processId).getLastVariableCleanup()).isEqualTo(
            previous
        );
        assertThat(count("ProcessVariable", processId)).isEqualTo(1);
        assertThat(count("TaskVariable", processId)).isEqualTo(1);
        assertThat(
            entityManager.find(IntegrationContextEntity.class, "integration-" + processId).getInBoundVariables()
        ).containsEntry("input", "secret");
        assertThat(
            entityManager.find(IntegrationContextEntity.class, "integration-" + processId).getOutBoundVariables()
        ).containsEntry("output", "secret");
        assertThat(entityManager.find(TaskEntity.class, "task-" + processId).getProcessVariables()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = { "completed", "cancelled" })
    void should_recordCleanupTimestamp_whenProcessHasNoPayloads(String state) {
        var process = new ProcessInstanceEntity();
        process.setId("empty");
        entityManager.persist(process);
        entityManager.flush();
        assertThat(process.getLastVariableCleanup()).isNull();
        var terminal = terminalEvent("empty", state);

        Instant started = Instant.now();
        new TransientRuntimeVariableCleanup(entityManager).cleanup(List.of(terminal));
        Instant finished = Instant.now();

        assertCleanupTimestamp("empty", started, finished);
        assertThat(count("ProcessVariable", "empty")).isZero();
        assertThat(count("TaskVariable", "empty")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = { "completed", "cancelled" })
    void should_refreshCleanupTimestamp_whenCleanupRunsAgain(String state) {
        seed("repeated");
        var terminal = terminalEvent("repeated", state);
        var cleanup = new TransientRuntimeVariableCleanup(entityManager);
        cleanup.cleanup(List.of(terminal));
        Instant previous = Instant.parse("2000-01-01T00:00:00Z");
        entityManager
            .createNativeQuery("update process_instance set last_variable_cleanup = :previous where id = 'repeated'")
            .setParameter("previous", previous.atOffset(ZoneOffset.UTC))
            .executeUpdate();
        entityManager.clear();

        Instant started = Instant.now();
        cleanup.cleanup(List.of(terminal));
        Instant finished = Instant.now();

        assertCleanupTimestamp("repeated", started, finished);
        assertThat(entityManager.find(ProcessInstanceEntity.class, "repeated").getLastVariableCleanup()).isAfter(
            previous
        );
    }

    private CloudRuntimeEvent<?, ?> terminalEvent(String processId, String state) {
        var process = new ProcessInstanceImpl();
        process.setId(processId);
        return state.equals("completed")
            ? new CloudProcessCompletedEventImpl(process)
            : new CloudProcessCancelledEventImpl(process);
    }

    private void assertCleanupTimestamp(String processId, Instant started, Instant finished) {
        var process = entityManager.find(ProcessInstanceEntity.class, processId);
        assertThat(process.getLastVariableCleanup()).isBetween(started.minusMillis(1), finished.plusMillis(1));
        var stored = (OffsetDateTime) entityManager
            .createNativeQuery("select last_variable_cleanup from process_instance where id = :id")
            .setParameter("id", processId)
            .getSingleResult();
        assertThat(stored.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(stored.toInstant()).isEqualTo(process.getLastVariableCleanup());
    }

    private long count(String entity, String processId) {
        return entityManager
            .createQuery("select count(v) from " + entity + " v where v.processInstanceId = :id", Long.class)
            .setParameter("id", processId)
            .getSingleResult();
    }

    private void seed(String processId) {
        var process = new ProcessInstanceEntity();
        process.setId(processId);
        entityManager.persist(process);
        var variable = new ProcessVariableEntity();
        variable.setName("payload");
        variable.setProcessInstanceId(processId);
        variable.setValue("secret");
        entityManager.persist(variable);
        var task = new TaskEntity();
        task.setId("task-" + processId);
        task.setProcessInstanceId(processId);
        task.getProcessVariables().add(variable);
        entityManager.persist(task);
        var local = new TaskVariableEntity();
        local.setName("local");
        local.setProcessInstanceId(processId);
        local.setTaskId(task.getId());
        local.setValue("local secret");
        entityManager.persist(local);
        var context = new IntegrationContextEntity();
        context.setId("integration-" + processId);
        context.setProcessInstanceId(processId);
        context.setInBoundVariables(new HashMap<>(Map.of("input", "secret")));
        context.setOutBoundVariables(new HashMap<>(Map.of("output", "secret")));
        context.setErrorMessage("execution error");
        entityManager.persist(context);
        entityManager.flush();
    }
}
