/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.function-router.enabled=true" })
public class QueryApplicationFunctionRouterIT extends QueryApplicationIT {

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings())
            .doesNotContainKeys("auditConsumer", "queryConsumer")
            .containsOnlyKeys("functionRouterInput", "functionRouterAnonymousInput", "producer");

        assertThat(bindingServiceProperties.getBindingProperties("functionRouterInput"))
            .extracting(BindingProperties::getGroup)
            .isEqualTo("consumer");

        assertThat(bindingServiceProperties.getBindingProperties("functionRouterAnonymousInput"))
            .extracting(BindingProperties::getGroup)
            .asString()
            .startsWith("consumer.");
    }

    @Test
    void functionRouter() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.isEnabled()).isTrue();

        assertThat(functionRouter.getFunctionRoutes())
            .containsOnly("auditConsumer", "queryConsumer", "graphQLEngineEventsConsumerSource");
        assertThat(functionRouter.destinations())
            .containsOnlyKeys("auditConsumer", "queryConsumer", "graphQLEngineEventsConsumerSource");
        assertThat(functionRouter.destinations("functionRouterInput"))
            .containsOnlyKeys("auditConsumer", "queryConsumer");
        assertThat(functionRouter.destinations("functionRouterAnonymousInput"))
            .containsOnlyKeys("graphQLEngineEventsConsumerSource");
        assertThat(functionRouter.registrations())
            .containsOnlyKeys("engineEvents")
            .satisfies(registrations ->
                assertThat(registrations.get("engineEvents"))
                    .containsOnly(
                        "queryConsumerFunction_registration",
                        "auditConsumerChannelHandlerConsumer_registration",
                        "engineEventsGraphQlSourceConsumer_registration"
                    )
                    .isNotEmpty()
            );
        assertThat(functionRouter.registrations("functionRouterInput"))
            .containsOnlyKeys("engineEvents")
            .satisfies(registrations ->
                assertThat(registrations.get("engineEvents"))
                    .containsOnly(
                        "queryConsumerFunction_registration",
                        "auditConsumerChannelHandlerConsumer_registration"
                    )
                    .isNotEmpty()
            );
        assertThat(functionRouter.registrations("functionRouterAnonymousInput"))
            .containsOnlyKeys("engineEvents")
            .satisfies(registrations ->
                assertThat(registrations.get("engineEvents"))
                    .containsOnly("engineEventsGraphQlSourceConsumer_registration")
                    .isNotEmpty()
            );
    }

    @Test
    @Override
    void rabbitQueues() {
        assertThat(queues)
            .isNotEmpty()
            .hasSize(2)
            .satisfies(map -> assertThat(map.keySet()).allMatch(key -> key.startsWith("consumer")));
    }

    @Test
    @Override
    void anonymousRabbitQueues() {
        assertThat(anonQueues).isEmpty();
    }

    @Test
    @Override
    void rabbitExchanges() {
        assertThat(exchanges).isNotEmpty().containsOnlyKeys("engineEvents");
    }
}
