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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.function-router.enabled=true" })
public class QueryRestApplicationFunctionRouterIT extends QueryRestApplicationIT {

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings())
            .doesNotContainKeys("auditConsumer", "queryConsumer")
            .containsOnlyKeys("functionRouterAnonymousInput", "producer");
    }

    @Test
    void functionRouter() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.isEnabled()).isTrue();

        assertThat(functionRouter.getRoutes()).containsOnlyKeys(
            "auditConsumer",
            "queryConsumer",
            "graphQLEngineEventsConsumerSource"
        );

        assertThat(functionRouter.destinations("functionRouterInput")).isEmpty();

        assertThat(functionRouter.destinations("functionRouterAnonymousInput")).containsOnly(
            Map.entry("graphQLEngineEventsConsumerSource", "queryEvents")
        );

        assertThat(functionRouter.registrations("functionRouterInput")).isEmpty();

        assertThat(functionRouter.registrations("functionRouterAnonymousInput"))
            .containsOnlyKeys("queryEvents")
            .satisfies(registrations ->
                assertThat(registrations.get("queryEvents"))
                    .containsOnly("engineEventsGraphQlSourceConsumer_registration")
                    .isNotEmpty()
            );
    }

    @Test
    void rabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getQueues()).satisfies(map ->
            assertThat(map.keySet())
                .isNotEmpty()
                .allMatch(key -> key.startsWith("consumer."))
        );
    }

    @Test
    void anonymousRabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getAnonymousQueues()).isEmpty();
    }
}
