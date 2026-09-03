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
package org.activiti.cloud.services.query.rest.subscriber;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.services.test.identity.JwtGraphQlClientInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.test.tester.WebSocketGraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

/**
 * The real end-to-end test for the pushed-counts feature: a real websocket connection,
 * authenticated with a real JWT, subscribing to a badge and receiving a count pushed through the
 * relay - modeled on {@code ActivitiGraphQLWsNativeStarterIT}'s
 * {@code EngineEventsSubscriptionTests} in activiti-cloud-notifications-graphql-service.
 *
 * <p>Messages are injected directly into the {@link Sinks.Many} bean that feeds the relay - there
 * is no real broker channel producing them yet.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = { PushedCountsWebSocketTestApplication.class },
    properties = { "activiti.features.query.pushed-counts.enabled=true" }
)
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PushedCountsWebSocketIT {

    private static final String WS_GRAPHQL_URI = "/v2/ws/graphql";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final Duration WEB_SOCKET_STOP_TIMEOUT = Duration.ofSeconds(5);
    private static final String TEST_USER = "testuser";

    @LocalServerPort
    private String port;

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private Sinks.Many<CountChangedMessage> pushedCountsSink;

    private WebSocketGraphQlTester graphQlTester;

    @BeforeEach
    void setUpGraphQlTester() throws Exception {
        URI url = new URI("ws://localhost:" + port + WS_GRAPHQL_URI);
        graphQlTester = WebSocketGraphQlTester.builder(url, new ReactorNettyWebSocketClient())
            .interceptor(
                new JwtGraphQlClientInterceptor(
                    identityTokenProducer.withTestUser(TEST_USER).withTestPassword("password")
                )
            )
            .build();
        graphQlTester.start().block(TIMEOUT);
    }

    @AfterEach
    void tearDownGraphQlTester() {
        if (graphQlTester != null) {
            graphQlTester.stop().block(WEB_SOCKET_STOP_TIMEOUT);
        }
    }

    /**
     * Proves the handshake/schema/interceptor wiring alone works - a live subscription with no
     * message sent - before any relay assertion depends on it too.
     */
    @Test
    @Order(1)
    void should_openTheSubscription_when_theConnectionIsAuthenticated() {
        Flux<Map> flux = graphQlTester
            .document("subscription { assignedTasks { count asOf } }")
            .executeSubscription()
            .toFlux("assignedTasks", Map.class);

        StepVerifier.create(flux).expectSubscription().thenAwait(Duration.ofMillis(300)).thenCancel().verify(TIMEOUT);
    }

    @Test
    @Order(2)
    void should_deliverTheCount_when_aMatchingMessageArrivesOnTheCountsChannel() {
        Flux<Map> flux = graphQlTester
            .document("subscription { assignedTasks { count asOf } }")
            .executeSubscription()
            .toFlux("assignedTasks", Map.class);

        Instant asOf = Instant.parse("2026-01-01T00:00:00Z");

        StepVerifier.create(flux)
            .expectSubscription()
            .thenAwait(Duration.ofMillis(300))
            .then(() -> sendCountChanged(new CountChangedMessage(ScopeKeys.assigned(TEST_USER), 3, asOf)))
            .expectNext(Map.of("count", 3, "asOf", asOf.toString()))
            .thenCancel()
            .verify(TIMEOUT);
    }

    @Test
    @Order(3)
    void should_notDeliverTheCount_when_theMessageIsForADifferentBadge() {
        Flux<Map> flux = graphQlTester
            .document("subscription { assignedTasks { count asOf } }")
            .executeSubscription()
            .toFlux("assignedTasks", Map.class);

        StepVerifier.create(flux)
            .expectSubscription()
            .thenAwait(Duration.ofMillis(300))
            .then(() -> sendCountChanged(new CountChangedMessage(ScopeKeys.queued(TEST_USER), 3, Instant.now())))
            .expectNoEvent(Duration.ofMillis(500))
            .thenCancel()
            .verify(TIMEOUT);
    }

    @Test
    @Order(4)
    void should_notDeliverTheCount_when_theMessageIsForADifferentUser() {
        Flux<Map> flux = graphQlTester
            .document("subscription { assignedTasks { count asOf } }")
            .executeSubscription()
            .toFlux("assignedTasks", Map.class);

        StepVerifier.create(flux)
            .expectSubscription()
            .thenAwait(Duration.ofMillis(300))
            .then(() -> sendCountChanged(new CountChangedMessage(ScopeKeys.assigned("someone-else"), 3, Instant.now())))
            .expectNoEvent(Duration.ofMillis(500))
            .thenCancel()
            .verify(TIMEOUT);
    }

    @Test
    @Order(5)
    void should_rejectTheConnection_when_itIsNotAuthenticated() throws Exception {
        WebSocketGraphQlTester unauthenticatedTester = WebSocketGraphQlTester.builder(
            new URI("ws://localhost:" + port + WS_GRAPHQL_URI),
            new ReactorNettyWebSocketClient()
        ).build();

        assertThatThrownBy(() -> unauthenticatedTester.start().block(TIMEOUT)).isNotNull();
    }

    private void sendCountChanged(CountChangedMessage message) {
        pushedCountsSink.tryEmitNext(message);
    }
}
