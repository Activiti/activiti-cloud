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
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.rabbitmq.prefix=${activiti.cloud.application.name}." })
public class QueryApplicationRabbitmqPrefixIT extends QueryApplicationIT {

    @Test
    void messagingRabbitMqPrefixProperties() {
        assertThat(messagingProperties.getRabbitmq().getPrefix()).isEqualTo("default-app.");
    }

    @Test
    void rabbitBinderDefaultPrefix() {
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.consumer.prefix", String.class))
            .isEqualTo("default-app.");

        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.producer.prefix", String.class))
            .isEqualTo("default-app.");
    }

    @Test
    @Override
    void rabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getQueues())
            .isNotEmpty()
            .hasSize(2)
            .containsOnlyKeys("default-app.engineEvents.query", "default-app.engineEvents.audit");
    }

    @Test
    @Override
    void anonymousRabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getAnonymousQueues())
            .isNotEmpty()
            .hasSize(1)
            .satisfies(map ->
                assertThat(map.keySet()).allMatch(key -> key.startsWith("default-app.engineEvents.anonymous."))
            );
    }

    @Test
    @Override
    void rabbitExchanges() {
        assertThat(binderFactoryListenerTestContext.getExchanges())
            .isNotEmpty()
            .containsOnlyKeys("default-app.engineEvents");
    }
}
