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
package org.activiti.cloud.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.activiti.cloud.services.query.app.ConsumerSubscriberRegistry;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQQueuesCleanupTestExecutionListener;
import org.activiti.cloud.services.test.liquibase.EnableCleanupLiquibaseAfterTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Verifies the liveness backstop over a real broker: an instance that stops heart-beating has its users
 * reclaimed once the instance-timeout elapses. Runs with short timing so the removal sweep fires within the
 * test; this instance heart-beats faster than the timeout, so only the silent simulated peer is expired.
 */
@SpringBootTest(
    classes = { QueryApplication.class },
    properties = {
        "identity.test.token-interceptor.enabled=false",
        "spring.sql.init.mode=always",
        "activiti.cloud.query.pushed-counts.enabled=true",
        "spring.cloud.stream.default-binder=rabbit",
        "activiti.cloud.query.pushed-counts.heartbeat-interval=PT1S",
        "activiti.cloud.query.pushed-counts.instance-timeout=PT5S",
        "activiti.cloud.query.pushed-counts.removal-interval=PT1S",
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
class SubscriberInstanceExpiryIT {

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .withReuse(true)
        .waitingFor(Wait.forListeningPort());

    @Autowired
    private ConsumerSubscriberRegistry consumerRegistry;

    @Autowired
    @Qualifier("subscriberRegistryProducer")
    private MessageChannel registryProducer;

    @Test
    void should_reclaimUsers_when_aPeerStopsHeartbeating() {
        registryProducer.send(
            new GenericMessage<>(
                SubscriberRegistryMessage.registered("mallory", List.of("eng"), "dying-instance", Instant.now())
            )
        );
        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("mallory")).isTrue());

        // the peer never heartbeats again; once its lastSeen is older than instance-timeout the sweep must drop it
        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("mallory")).isFalse());
    }
}
