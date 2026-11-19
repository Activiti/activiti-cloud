package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.*;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.*;
import org.activiti.cloud.services.query.model.*;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.junit.jupiter.api.*;
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
        var cleanup = new TransientQueryPayloadCleanup(entityManager);

        cleanup.cleanup(List.of(terminal, terminal));

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

        new TransientQueryPayloadCleanup(entityManager).cleanup(List.of(new CloudProcessStartedEventImpl(process)));

        assertThat(count("ProcessVariable", "running")).isEqualTo(1);
        assertThat(count("TaskVariable", "running")).isEqualTo(1);
    }

    @Test
    void should_rollbackPayloadCleanupWithTransaction() {
        seed("rollback");
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
        var process = new ProcessInstanceImpl();
        process.setId("rollback");

        new TransientQueryPayloadCleanup(entityManager).cleanup(List.of(new CloudProcessCompletedEventImpl(process)));
        entityManager.getTransaction().rollback();
        entityManager.clear();
        entityManager.getTransaction().begin();

        assertThat(count("ProcessVariable", "rollback")).isEqualTo(1);
        assertThat(
            entityManager.find(IntegrationContextEntity.class, "integration-rollback").getInBoundVariables()
        ).containsEntry("input", "secret");
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
