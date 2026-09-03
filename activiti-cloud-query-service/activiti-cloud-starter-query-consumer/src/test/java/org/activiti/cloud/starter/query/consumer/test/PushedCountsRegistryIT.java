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
package org.activiti.cloud.starter.query.consumer.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.activiti.cloud.services.query.app.ConsumerSubscriberRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * End-to-end binding test for the pushed-counts subscriber registry: drives real broker messages
 * through the in-memory test binder so the {@code subscriberRegistry} binding, the JSON
 * (de)serialization of {@link org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage},
 * the feature toggle and the startup resync are all exercised together — none of which the unit
 * tests cover.
 */
@SpringBootTest(
    classes = QueryConsumerTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "activiti.cloud.services.oauth2.iam-name=test",
        "activiti.features.query.pushed-counts.enabled=true",
    }
)
@EnableTestBinder
class PushedCountsRegistryIT {

    private static final String REGISTRY_DESTINATION = "subscriberRegistry";
    private static final String SENT_AT = "2026-01-01T00:00:00Z";

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Autowired
    private ConsumerSubscriberRegistry registry;

    @Test
    void registeredMessage_isDeserializedAndAppliedToTheRegistry() {
        input.send(registryJson("""
            {"type":"REGISTERED","userId":"alice","groups":["eng"],"sourceId":"rest-1","sentAt":"%s"}
            """.formatted(SENT_AT)), REGISTRY_DESTINATION);

        assertThat(registry.isWatching("alice")).isTrue();
        assertThat(registry.groupsOf("alice")).containsExactly("eng");
    }

    @Test
    void unregisteredMessage_removesTheUserHeldByThatInstance() {
        input.send(registryJson("""
            {"type":"REGISTERED","userId":"bob","groups":["fin"],"sourceId":"rest-1","sentAt":"%s"}
            """.formatted(SENT_AT)), REGISTRY_DESTINATION);
        assertThat(registry.isWatching("bob")).isTrue();

        input.send(registryJson("""
            {"type":"UNREGISTERED","userId":"bob","sourceId":"rest-1","sentAt":"%s"}
            """.formatted(SENT_AT)), REGISTRY_DESTINATION);

        assertThat(registry.isWatching("bob")).isFalse();
    }

    @Test
    void consumerBroadcastsResyncRequest_onStartup() {
        Message<byte[]> resync = output.receive(5000, REGISTRY_DESTINATION);

        assertThat(resync).isNotNull();
        assertThat(new String(resync.getPayload(), StandardCharsets.UTF_8)).contains("RESYNC_REQUEST");
    }

    @Test
    void snapshotMessage_withNestedEntries_isDeserializedAndMerged() {
        input.send(registryJson("""
            {"type":"SNAPSHOT","entries":[{"userId":"carol","groups":["eng"]},\
            {"userId":"dave","groups":["fin","ops"]}],"sourceId":"rest-9","sentAt":"%s"}
            """.formatted(SENT_AT)), REGISTRY_DESTINATION);

        assertThat(registry.isWatching("carol")).isTrue();
        assertThat(registry.sourcesOf("carol")).containsExactly("rest-9");
        assertThat(registry.groupsOf("dave")).containsExactlyInAnyOrder("fin", "ops");
    }

    @Test
    void registrationsFromTwoInstances_areAggregatedForTheSameUser() {
        input.send(registryJson("""
            {"type":"REGISTERED","userId":"erin","groups":["eng"],"sourceId":"rest-1","sentAt":"%s"}
            """.formatted(SENT_AT)), REGISTRY_DESTINATION);
        input.send(registryJson("""
            {"type":"REGISTERED","userId":"erin","groups":["eng"],"sourceId":"rest-2","sentAt":"%s"}
            """.formatted(SENT_AT)), REGISTRY_DESTINATION);

        assertThat(registry.sourcesOf("erin")).containsExactlyInAnyOrder("rest-1", "rest-2");
    }

    private static Message<byte[]> registryJson(String json) {
        return MessageBuilder
            .withPayload(json.strip().getBytes(StandardCharsets.UTF_8))
            .setHeader("contentType", "application/json")
            .build();
    }
}
