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
import java.util.List;
import java.util.UUID;
import org.activiti.api.runtime.model.impl.BPMNActivityImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.conf.QueryConsumerPartitionedChannelKeySelector;
import org.activiti.cloud.services.query.app.TwoPodsRaceSupport.Outcome;
import org.activiti.cloud.services.query.app.TwoPodsRaceSupport.Pod;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class TwoPodsQueryConsumerRaceIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    private static StandardServiceRegistry registryA;
    private static StandardServiceRegistry registryB;
    private static SessionFactory sessionFactoryA;
    private static SessionFactory sessionFactoryB;

    @BeforeAll
    static void setUpTwoIndependentEntityManagerFactoriesAgainstTheSameDatabase() {
        registryA = TwoPodsRaceSupport.registry(postgres, "create-drop");
        sessionFactoryA = TwoPodsRaceSupport.buildSessionFactory(registryA);

        registryB = TwoPodsRaceSupport.registry(postgres, "none");
        sessionFactoryB = TwoPodsRaceSupport.buildSessionFactory(registryB);
    }

    @AfterAll
    static void tearDown() {
        if (sessionFactoryB != null) {
            sessionFactoryB.close();
        }
        if (sessionFactoryA != null) {
            sessionFactoryA.close();
        }
        if (registryB != null) {
            StandardServiceRegistryBuilder.destroy(registryB);
        }
        if (registryA != null) {
            StandardServiceRegistryBuilder.destroy(registryA);
        }
    }

    @Test
    void should_notRaiseConstraintViolation_when_twoIndependentPodsRaceThroughTheRealPartitionedChannel()
        throws Exception {
        String processInstanceId = UUID.randomUUID().toString();
        String elementId = "Activity_1le21gy";
        String executionId = UUID.randomUUID().toString();
        String conflictingId = processInstanceId + ":" + elementId + ":" + executionId;

        Pod podA = new Pod(sessionFactoryA);
        Pod podB = new Pod(sessionFactoryB);

        podA.send(startedEventMessage(processInstanceId, elementId, executionId));
        podB.send(startedEventMessage(processInstanceId, elementId, executionId));

        Outcome outcomeA = podA.awaitOutcome();
        Outcome outcomeB = podB.awaitOutcome();
        List<Outcome> outcomes = List.of(outcomeA, outcomeB);

        for (Outcome outcome : outcomes) {
            assertThat(outcome.committed())
                .as(
                    "oba pody powinny zakończyć się commitem dzięki acquireCrossPodLock - " +
                        "drugi po prostu czeka na commit pierwszego zamiast wpadać w duplicate key; " +
                        "błąd (jeśli jest): %s",
                    outcome.exception()
                )
                .isTrue();
        }

        try (EntityManager verifyEm = sessionFactoryA.createEntityManager()) {
            BPMNActivityEntity persisted = verifyEm.find(BPMNActivityEntity.class, conflictingId);
            assertThat(persisted).as("dla konfliktowego id musi istnieć dokładnie jeden wiersz").isNotNull();
            assertThat(persisted.getStatus())
                .as("oba eventy to identyczny ACTIVITY_STARTED, więc końcowy status musi być STARTED")
                .isEqualTo(BPMNActivityEntity.BPMNActivityStatus.STARTED);
        }
    }

    private Message<List<CloudRuntimeEvent<?, ?>>> startedEventMessage(
        String processInstanceId,
        String elementId,
        String executionId
    ) {
        BPMNActivityImpl activity = new BPMNActivityImpl(elementId, "Create Body", "exclusiveGateway");
        activity.setProcessInstanceId(processInstanceId);
        activity.setExecutionId(executionId);
        activity.setProcessDefinitionId("Process_1772708583320:91:def-1");

        CloudBPMNActivityStartedEventImpl event = new CloudBPMNActivityStartedEventImpl(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            activity,
            activity.getProcessDefinitionId(),
            processInstanceId
        );

        List<CloudRuntimeEvent<?, ?>> payload = List.of(event);

        return MessageBuilder.withPayload(payload)
            .setHeader(QueryConsumerPartitionedChannelKeySelector.ROOT_PROCESS_INSTANCE_ID, processInstanceId)
            .build();
    }
}
