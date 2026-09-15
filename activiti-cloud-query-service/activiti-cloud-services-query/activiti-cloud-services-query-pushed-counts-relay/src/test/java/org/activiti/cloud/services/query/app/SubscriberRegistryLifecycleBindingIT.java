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
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;

/**
 * Lifecycle binding tests for the relay's startup and scheduled broadcasts. A short heartbeat interval
 * makes the periodic HEARTBEAT observable within the test window, which - together with the startup
 * SNAPSHOT - proves {@code @EnableScheduling} and the {@code ApplicationReadyEvent} listener are
 * actually wired: dropping {@code @EnableScheduling} still compiles and passes every other test, yet
 * silently stops heartbeats. The startup SNAPSHOT is a one-shot emitted before any test runs, so it is
 * asserted first (via method ordering) before other traffic drains it.
 */
@SpringBootTest(
    classes = PushedCountsRelayTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "activiti.cloud.query.pushed-counts.enabled=true",
        "activiti.cloud.query.pushed-counts.instance-id=lifecycle-instance",
        "activiti.cloud.query.pushed-counts.heartbeat-interval=PT0.2S",
    }
)
@EnableTestBinder
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SubscriberRegistryLifecycleBindingIT {

    private static final String REGISTRY_DESTINATION = "subscriberRegistry";

    @Autowired
    private OutputDestination output;

    @Test
    @Order(1)
    void broadcastsSnapshotOnStartup() {
        String snapshot = awaitMessageContaining("\"type\":\"SNAPSHOT\"");

        assertThat(snapshot)
            .isNotNull()
            .contains("\"userId\":\"alice\"")
            .contains("\"sourceId\":\"lifecycle-instance\"");
    }

    @Test
    @Order(2)
    void broadcastsHeartbeatsPeriodically() {
        String heartbeat = awaitMessageContaining("\"type\":\"HEARTBEAT\"");

        assertThat(heartbeat).isNotNull().contains("\"sourceId\":\"lifecycle-instance\"");
    }

    private String awaitMessageContaining(String token) {
        AtomicReference<String> found = new AtomicReference<>();
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> {
                Message<byte[]> received = output.receive(100, REGISTRY_DESTINATION);
                while (received != null) {
                    String body = new String(received.getPayload(), StandardCharsets.UTF_8);
                    if (body.contains(token)) {
                        found.set(body);
                        return true;
                    }
                    received = output.receive(100, REGISTRY_DESTINATION);
                }
                return false;
            });
        return found.get();
    }
}
