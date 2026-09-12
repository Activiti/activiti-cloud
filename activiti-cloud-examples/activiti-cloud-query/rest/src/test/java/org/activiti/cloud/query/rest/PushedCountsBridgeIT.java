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
package org.activiti.cloud.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQQueuesCleanupTestExecutionListener;
import org.activiti.cloud.services.test.liquibase.EnableCleanupLiquibaseAfterTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Flux;

/**
 * Proves the count bridge over a real broker: a message sent to the {@code pushedCounts} destination
 * arrives at {@code countConsumer} and lands on this instance's {@code pushedCountsFlux}.
 */
@SpringBootTest(
    classes = { QueryRestApplication.class },
    properties = {
        "identity.test.token-interceptor.enabled=false",
        "spring.sql.init.mode=always",
        "activiti.cloud.query.pushed-counts.enabled=true",
        "activiti.features.query.pushed-counts.enabled=true",
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(
    initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class }
)
@TestExecutionListeners(
    value = RabbitMQQueuesCleanupTestExecutionListener.class,
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS
)
@EnableCleanupLiquibaseAfterTest
@ResourceLocks(value = { @ResourceLock("postgres"), @ResourceLock("rabbitmq") })
class PushedCountsBridgeIT {

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .withReuse(true)
        .waitingFor(Wait.forListeningPort());

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private Flux<CountChangedMessage> pushedCountsFlux;

    @Test
    void messageSentOnTheBroker_arrivesOnThisInstancesPushedCountsFlux() {
        List<CountChangedMessage> received = new CopyOnWriteArrayList<>();
        pushedCountsFlux.subscribe(received::add);

        CountChangedMessage sent = new CountChangedMessage("assigned:alice", 5, Instant.now());
        boolean accepted = streamBridge.send("pushedCounts", sent);
        assertThat(accepted).isTrue();

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(received).contains(sent));
    }
}
