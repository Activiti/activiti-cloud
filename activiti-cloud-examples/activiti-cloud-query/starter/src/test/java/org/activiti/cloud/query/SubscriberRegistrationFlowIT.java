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
import java.util.Set;
import org.activiti.cloud.services.query.app.ConsumerSubscriberRegistry;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberRegistry;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Exercises the subscriber presence flows across both halves of the all-in-one query service over a real
 * broker: a REST-side registration round-trips through RabbitMQ into the consumer-side
 * {@link ConsumerSubscriberRegistry}, groups carry across, a user held by two instances is dropped only
 * when both release, and resync snapshots reconcile (a stale one never resurrects an unregistered user).
 * Peer instances are simulated by publishing {@link SubscriberRegistryMessage}s onto the shared destination.
 */
@SpringBootTest(
    classes = { QueryApplication.class },
    properties = {
        "identity.test.token-interceptor.enabled=false",
        "spring.sql.init.mode=always",
        "activiti.cloud.messaging.function-router.enabled=true",
        "activiti.cloud.query.pushed-counts.enabled=true",
        "spring.cloud.stream.default-binder=rabbit",
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@EnableCleanupLiquibaseAfterTest
@ResourceLocks(value = { @ResourceLock("postgres"), @ResourceLock("rabbitmq") })
class SubscriberRegistrationFlowIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @ServiceConnection
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine").withReuse(true);

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .withReuse(true)
        .waitingFor(Wait.forListeningPort());

    @Autowired
    private SubscriberRegistry restRegistry;

    @Autowired
    private ConsumerSubscriberRegistry consumerRegistry;

    // Publishes onto the shared subscriberRegistry destination to mimic another instance's broadcasts.
    @Autowired
    @Qualifier("subscriberRegistryProducer")
    private MessageChannel registryProducer;

    @Test
    void should_propagateRegistrationAndUnregistration_when_aUserSubscribesOnRest() {
        restRegistry.register("alice", Set.of("eng"), "session-1", Instant.now());
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("alice")).isTrue());

        restRegistry.unregister("alice", "session-1", Instant.now());
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("alice")).isFalse());
    }

    @Test
    void should_carryGroupsToTheConsumerRegistry_when_aUserSubscribesOnRest() {
        restRegistry.register("bob", Set.of("eng", "sales"), "session-1", Instant.now());

        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> {
                assertThat(consumerRegistry.isWatching("bob")).isTrue();
                assertThat(consumerRegistry.groupsOf("bob")).containsExactlyInAnyOrder("eng", "sales");
            });

        restRegistry.unregister("bob", "session-1", Instant.now());
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("bob")).isFalse());
    }

    @Test
    void should_keepWatchingUntilBothInstancesRelease_when_aUserIsHeldByTwoInstances() {
        restRegistry.register("dave", Set.of("eng"), "session-1", Instant.now());
        peerSends(SubscriberRegistryMessage.registered("dave", List.of("eng"), "peer-instance-A", Instant.now()));
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.sourcesOf("dave")).hasSize(2).contains("peer-instance-A"));

        // this instance releases dave, but the peer still holds him
        restRegistry.unregister("dave", "session-1", Instant.now());
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.sourcesOf("dave")).containsExactly("peer-instance-A"));

        peerSends(SubscriberRegistryMessage.unregistered("dave", "peer-instance-A", Instant.now()));
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("dave")).isFalse());
    }

    @Test
    void should_reconcileAwayAnAbsentUser_when_aNewerResyncSnapshotArrives() {
        Instant firstSnapshot = Instant.now();
        peerSends(
            SubscriberRegistryMessage.snapshot(
                List.of(
                    new SubscriberRegistryMessage.Entry("erin", List.of("eng")),
                    new SubscriberRegistryMessage.Entry("frank", List.of("eng"))
                ),
                "peer-instance-B",
                firstSnapshot
            )
        );
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> {
                assertThat(consumerRegistry.isWatching("erin")).isTrue();
                assertThat(consumerRegistry.isWatching("frank")).isTrue();
            });

        // a later snapshot from the same instance drops frank -> the consumer must reconcile him away
        peerSends(
            SubscriberRegistryMessage.snapshot(
                List.of(new SubscriberRegistryMessage.Entry("erin", List.of("eng"))),
                "peer-instance-B",
                firstSnapshot.plusSeconds(10)
            )
        );
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> {
                assertThat(consumerRegistry.isWatching("frank")).isFalse();
                assertThat(consumerRegistry.isWatching("erin")).isTrue();
            });
    }

    @Test
    void should_notResurrectAnUnregisteredUser_when_aStaleResyncSnapshotArrives() {
        Instant registered = Instant.now();
        Instant staleSnapshot = registered.plusSeconds(5); // captured before the unregister, delivered after it
        Instant unregistered = registered.plusSeconds(10);

        peerSends(SubscriberRegistryMessage.registered("grace", List.of("eng"), "peer-instance-C", registered));
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("grace")).isTrue());

        peerSends(SubscriberRegistryMessage.unregistered("grace", "peer-instance-C", unregistered));
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("grace")).isFalse());

        // the stale snapshot still lists grace but carries an older timestamp than the unregister
        peerSends(
            SubscriberRegistryMessage.snapshot(
                List.of(new SubscriberRegistryMessage.Entry("grace", List.of("eng"))),
                "peer-instance-C",
                staleSnapshot
            )
        );
        // the marker is delivered after the stale snapshot on the same channel; once it lands, the snapshot was applied
        peerSends(
            SubscriberRegistryMessage.registered("heidi", List.of("eng"), "peer-instance-C", registered.plusSeconds(20))
        );
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("heidi")).isTrue());

        assertThat(consumerRegistry.isWatching("grace")).isFalse();
    }

    @Test
    void should_dropTheUserOnlyAfterTheLastSession_when_aUserHasTwoRestSessions() {
        restRegistry.register("nina", Set.of("eng"), "session-1", Instant.now());
        restRegistry.register("nina", Set.of("eng"), "session-2", Instant.now());
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("nina")).isTrue());

        // releasing one of two sessions is not an empty transition, so nothing is broadcast and nina stays watched
        restRegistry.unregister("nina", "session-1", Instant.now());
        assertThat(consumerRegistry.isWatching("nina")).isTrue();

        restRegistry.unregister("nina", "session-2", Instant.now());
        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("nina")).isFalse());
    }

    @Test
    void should_notBlockALaterRegistration_when_anEarlyUnregisterArrivesFirst() {
        Instant early = Instant.now();
        // an out-of-order UNREGISTERED for a user never seen must be harmless and must not veto a newer REGISTERED
        peerSends(SubscriberRegistryMessage.unregistered("ivan", "peer-instance-D", early));
        peerSends(
            SubscriberRegistryMessage.registered("ivan", List.of("eng"), "peer-instance-D", early.plusSeconds(5))
        );

        await()
            .atMost(TIMEOUT)
            .untilAsserted(() -> assertThat(consumerRegistry.isWatching("ivan")).isTrue());
    }

    private void peerSends(SubscriberRegistryMessage message) {
        registryProducer.send(new GenericMessage<>(message));
    }
}
