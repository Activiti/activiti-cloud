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
package org.activiti.cloud.services.query.app;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.activiti.api.runtime.model.impl.BPMNActivityImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.services.query.events.handlers.BPMNActivityStartedEventHandler;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class BPMNActivityStartedEventHandlerConcurrencyIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    @BeforeAll
    static void setUpSessionFactory() {
        registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.connection.url", postgres.getJdbcUrl())
            .applySetting("hibernate.connection.username", postgres.getUsername())
            .applySetting("hibernate.connection.password", postgres.getPassword())
            .applySetting("hibernate.connection.driver_class", "org.postgresql.Driver")
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.hbm2ddl.auto", "create-drop")
            .applySetting(
                "hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
            )
            .build();

        Metadata metadata = new MetadataSources(registry).addAnnotatedClass(BPMNActivityEntity.class).buildMetadata();

        sessionFactory = metadata.buildSessionFactory();
    }

    @AfterAll
    static void tearDownSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void should_notRaiseConstraintViolation_when_twoConcurrentSessionsRaceOnFindThenCreate() throws Exception {
        String processInstanceId = UUID.randomUUID().toString();
        String elementId = "Activity_1le21gy";
        String executionId = UUID.randomUUID().toString();
        String conflictingId = processInstanceId + ":" + elementId + ":" + executionId;

        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<Outcome> raceParticipant = () -> {
            EntityManager entityManager = sessionFactory.createEntityManager();
            EntityTransaction tx = entityManager.getTransaction();
            try {
                tx.begin();

                BPMNActivityStartedEventHandler handler = new BPMNActivityStartedEventHandler(entityManager);
                handler.handle(startedEvent(processInstanceId, elementId, executionId));

                entityManager.flush();
                tx.commit();
                return Outcome.ok();
            } catch (Exception ex) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                return Outcome.failure(ex);
            } finally {
                entityManager.close();
            }
        };

        Future<Outcome> first = pool.submit(raceParticipant);
        Future<Outcome> second = pool.submit(raceParticipant);

        List<Outcome> outcomes = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        pool.shutdownNow();

        for (Outcome outcome : outcomes) {
            assertThat(outcome.committed())
                .as(
                    "obie transakcje powinny zakończyć się commitem dzięki acquireCrossPodLock - " +
                        "błąd (jeśli jest): %s",
                    outcome.exception()
                )
                .isTrue();
        }

        try (EntityManager verifyEm = sessionFactory.createEntityManager()) {
            BPMNActivityEntity persisted = verifyEm.find(BPMNActivityEntity.class, conflictingId);
            assertThat(persisted).as("dla konfliktowego id musi istnieć dokładnie jeden wiersz").isNotNull();
            assertThat(persisted.getStatus())
                .as("oba eventy to identyczny ACTIVITY_STARTED, więc końcowy status musi być STARTED")
                .isEqualTo(BPMNActivityEntity.BPMNActivityStatus.STARTED);
        }
    }

    private CloudBPMNActivityStartedEventImpl startedEvent(
        String processInstanceId,
        String elementId,
        String executionId
    ) {
        BPMNActivityImpl activity = new BPMNActivityImpl(elementId, "Create Body", "exclusiveGateway");
        activity.setProcessInstanceId(processInstanceId);
        activity.setExecutionId(executionId);
        activity.setProcessDefinitionId("Process_1772708583320:91:def-1");

        return new CloudBPMNActivityStartedEventImpl(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            activity,
            activity.getProcessDefinitionId(),
            processInstanceId
        );
    }

    private record Outcome(boolean committed, Exception exception) {
        static Outcome ok() {
            return new Outcome(true, null);
        }

        static Outcome failure(Exception exception) {
            return new Outcome(false, exception);
        }
    }
}
