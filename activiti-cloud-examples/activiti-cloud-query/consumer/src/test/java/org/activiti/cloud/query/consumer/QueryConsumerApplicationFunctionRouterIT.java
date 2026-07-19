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
package org.activiti.cloud.query.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.function-router.enabled=true" })
public class QueryConsumerApplicationFunctionRouterIT extends QueryConsumerApplicationIT {

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings())
            .doesNotContainKeys("auditConsumer", "queryConsumer")
            .containsOnlyKeys("functionRouterInput", "producer", "queryEventsProducer");

        assertThat(bindingServiceProperties.getBindingProperties("functionRouterInput")).satisfies(binding -> {
            assertThat(binding.getGroup()).isEqualTo("consumer");
            assertThat(binding.getConsumer()).isNotNull();
            assertThat(binding.getConsumer().getConcurrency()).isEqualTo(1);
        });
    }

    @Test
    void functionRouter() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.isEnabled()).isTrue();

        assertThat(functionRouter.getFunctionRoutes()).containsOnly("auditConsumer", "queryConsumer");
        assertThat(functionRouter.destinations("functionRouterInput")).containsOnlyKeys(
            "auditConsumer",
            "queryConsumer"
        );
        assertThat(functionRouter.destinations("functionRouterAnonymousInput")).isEmpty();
        assertThat(functionRouter.registrations("functionRouterInput"))
            .containsOnlyKeys("engineEvents")
            .satisfies(registrations -> {
                assertThat(registrations.get("engineEvents"))
                    .containsOnly(
                        "queryConsumerFunction_registration",
                        "auditConsumerChannelHandlerConsumer_registration"
                    )
                    .isNotEmpty();
            });
        assertThat(functionRouter.registrations("functionRouterAnonymousInput")).isEmpty();
    }

    @Test
    @Override
    void rabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getQueues()).isNotEmpty().hasSize(1).containsOnlyKeys("consumer");
    }
}
