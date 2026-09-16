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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import org.activiti.cloud.services.query.subscription.SubscriberWentLiveEvent;
import org.activiti.cloud.services.query.subscription.SubscriberWentQuietEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * End-to-end binding test for the subscriber-registry producer half of the relay: drives messages
 * through the in-memory test binder so the {@code subscriberRegistry} output/input bindings, the JSON
 * serialization of the registry message and the resync round-trip are all exercised together - proving
 * presence actually reaches the shared destination via a declared channel, which the unit tests (mock
 * channel) cannot.
 */
@SpringBootTest(
    classes = PushedCountsRelayTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = { "activiti.cloud.query.pushed-counts.enabled=true" }
)
@EnableTestBinder
class SubscriberRegistryProducerBindingIT {

    private static final String REGISTRY_DESTINATION = "subscriberRegistry";
    private static final Instant SENT_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Autowired
    private ApplicationEventPublisher events;

    @BeforeEach
    void drainStartupSnapshot() {
        // The broadcaster emits a SNAPSHOT on ApplicationReadyEvent; clear it so each test sees only its own message.
        while (output.receive(300, REGISTRY_DESTINATION) != null) {
            // discard
        }
    }

    @Test
    void broadcastsRegistered_whenAUserGoesLive() {
        events.publishEvent(new SubscriberWentLiveEvent("alice", Set.of("eng"), SENT_AT));

        String message = receiveContaining("\"type\":\"REGISTERED\"");

        assertThat(message).isNotNull().contains("\"userId\":\"alice\"").contains("eng").contains("\"sourceId\":\"");
    }

    @Test
    void broadcastsUnregistered_whenAUserGoesQuiet() {
        events.publishEvent(new SubscriberWentQuietEvent("alice", SENT_AT));

        String message = receiveContaining("\"type\":\"UNREGISTERED\"");

        assertThat(message).isNotNull().contains("\"userId\":\"alice\"").contains("\"sourceId\":\"");
    }

    @Test
    void repliesWithSnapshot_toAResyncRequestFromAnotherInstance() {
        // Startup snapshot was drained in @BeforeEach; confirm the channel is quiet so the SNAPSHOT
        // asserted below can only be the reply to our request, not a leftover startup snapshot.
        assertThat(output.receive(300, REGISTRY_DESTINATION)).isNull();

        input.send(
            registryJson(
                """
                {"type":"RESYNC_REQUEST","sourceId":"consumer-1","sentAt":"%s"}
                """.formatted(SENT_AT)
            ),
            REGISTRY_DESTINATION
        );

        String reply = receiveContaining("\"type\":\"SNAPSHOT\"");

        assertThat(reply).isNotNull().contains("\"userId\":\"alice\"");
    }

    private String receiveContaining(String token) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Message<byte[]> received = output.receive(5000, REGISTRY_DESTINATION);
            if (received == null) {
                return null;
            }
            String body = new String(received.getPayload(), StandardCharsets.UTF_8);
            if (body.contains(token)) {
                return body;
            }
        }
        return null;
    }

    private static Message<byte[]> registryJson(String json) {
        return MessageBuilder.withPayload(json.strip().getBytes(StandardCharsets.UTF_8))
            .setHeader("contentType", "application/json")
            .build();
    }
}
