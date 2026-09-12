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
import org.springframework.messaging.support.MessageBuilder;

/**
 * End-to-end proof that the whole registry path stays inert while the pushed-counts toggle is off:
 * a message crosses the binding but the consumer drops it, so the registry never fills.
 */
@SpringBootTest(
    classes = QueryConsumerTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "activiti.cloud.services.oauth2.iam-name=test", "activiti.features.query.pushed-counts.enabled=false",
    }
)
@EnableTestBinder
class PushedCountsRegistryDisabledIT {

    @Autowired
    private InputDestination input;

    @Autowired
    private ConsumerSubscriberRegistry registry;

    @Test
    void registeredMessage_isIgnored_whenFeatureDisabled() {
        input.send(
            MessageBuilder.withPayload(
                """
                {"type":"REGISTERED","userId":"frank","groups":["eng"],"sourceId":"rest-1","sentAt":"2026-01-01T00:00:00Z"}
                """.strip()
                    .getBytes(StandardCharsets.UTF_8)
            )
                .setHeader("contentType", "application/json")
                .build(),
            "subscriberRegistry"
        );

        assertThat(registry.isWatching("frank")).isFalse();
        assertThat(registry.size()).isZero();
    }
}
