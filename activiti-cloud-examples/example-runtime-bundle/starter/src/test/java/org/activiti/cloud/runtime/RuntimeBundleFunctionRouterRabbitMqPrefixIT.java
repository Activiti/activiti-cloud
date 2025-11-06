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
package org.activiti.cloud.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.rabbitmq.prefix=${activiti.cloud.application.name}." })
public class RuntimeBundleFunctionRouterRabbitMqPrefixIT extends RuntimeBundleFunctionRouterEnabledIT {

    @Test
    void messagingProperties() {
        assertThat(messagingProperties.getRabbitmq().getPrefix()).isEqualTo("myapp.");
    }

    @Test
    void rabbitBinderDefaultPrefix() {
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.consumer.prefix", String.class))
            .isEqualTo("myapp.");

        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.producer.prefix", String.class))
            .isEqualTo("myapp.");
    }

    @Test
    @Override
    void bindingServicePropertiesRequiredProducerGroups() {
        assertThat(bindingServiceProperties.getProducerProperties("signalProducer").getRequiredGroups()).isEmpty();
        assertThat(bindingServiceProperties.getProducerProperties("messageEventsOutput").getRequiredGroups()).isEmpty();
        assertThat(bindingServiceProperties.getProducerProperties("auditProducer").getRequiredGroups())
            .containsOnly("myapp.consumer");
    }
}
