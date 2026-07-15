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
package org.activiti.cloud.common.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.ConfigurableEnvironment;

class ConnectorOutputBindingEnvironmentPostProcessorTest {

    private static final String OUTPUT_BINDINGS_KEY = "spring.cloud.stream.output-bindings";

    private final ConnectorOutputBindingEnvironmentPostProcessor processor =
        new ConnectorOutputBindingEnvironmentPostProcessor();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withInitializer(context ->
        processor.postProcessEnvironment(context.getEnvironment(), new SpringApplication())
    );

    @Test
    void should_doNothing_when_noConnectorsConfigured() {
        contextRunner.run(context -> {
            ConfigurableEnvironment environment = context.getEnvironment();
            assertThat(environment.getPropertySources().contains(propertySourceName())).isFalse();
            assertThat(environment.getProperty(OUTPUT_BINDINGS_KEY)).isNull();
        });
    }

    @Test
    void should_contributeDestinationRequiredGroupsAndOutputBinding_forSingleConnector() {
        contextRunner
            .withPropertyValues(
                "activiti.cloud.messaging.connectors.myConsumer.destination=orders",
                "activiti.cloud.messaging.connectors.myConsumer.required-groups=worker-a",
                "activiti.cloud.messaging.connectors.myConsumer.queue-name-group-only=true"
            )
            .run(context -> {
                ConfigurableEnvironment environment = context.getEnvironment();
                assertThat(environment.getProperty("spring.cloud.stream.bindings.myConsumer.destination")).isEqualTo(
                    "orders"
                );
                assertThat(
                    environment.getProperty("spring.cloud.stream.bindings.myConsumer.producer.required-groups")
                ).isEqualTo("worker-a");
                assertThat(
                    environment.getProperty(
                        "spring.cloud.stream.rabbit.bindings.myConsumer.producer.queue-name-group-only"
                    )
                ).isEqualTo("true");
                assertThat(environment.getProperty(OUTPUT_BINDINGS_KEY)).isEqualTo("myConsumer");
            });
    }

    @Test
    void should_joinMultipleRequiredGroupsWithComma() {
        contextRunner
            .withPropertyValues(
                "activiti.cloud.messaging.connectors.fanout.destination=orders",
                "activiti.cloud.messaging.connectors.fanout.required-groups=worker-a,worker-b,worker-c",
                "activiti.cloud.messaging.connectors.fanout.queue-name-group-only=true"
            )
            .run(context ->
                assertThat(
                    context.getEnvironment().getProperty("spring.cloud.stream.bindings.fanout.producer.required-groups")
                ).isEqualTo("worker-a,worker-b,worker-c")
            );
    }

    @Test
    void should_notContributeQueueNameGroupOnly_when_flagIsFalseOrAbsent() {
        contextRunner
            .withPropertyValues(
                "activiti.cloud.messaging.connectors.plain.destination=orders",
                "activiti.cloud.messaging.connectors.plain.required-groups=worker-a"
            )
            .run(context -> {
                ConfigurableEnvironment environment = context.getEnvironment();
                assertThat(
                    environment.getProperty("spring.cloud.stream.rabbit.bindings.plain.producer.queue-name-group-only")
                ).isNull();
                assertThat(environment.getProperty(OUTPUT_BINDINGS_KEY)).isEqualTo("plain");
            });
    }

    @Test
    void should_registerAllConnectorsInOutputBindings() {
        contextRunner
            .withPropertyValues(
                "activiti.cloud.messaging.connectors.first.destination=orders-create",
                "activiti.cloud.messaging.connectors.first.required-groups=worker",
                "activiti.cloud.messaging.connectors.second.destination=orders-update",
                "activiti.cloud.messaging.connectors.second.required-groups=worker"
            )
            .run(context ->
                assertThat(
                    Objects.requireNonNull(context.getEnvironment().getProperty(OUTPUT_BINDINGS_KEY)).split(";")
                ).containsExactlyInAnyOrder("first", "second")
            );
    }

    @Test
    void should_appendToExistingOutputBindings_withoutDuplicating() {
        contextRunner
            .withPropertyValues(
                "spring.cloud.stream.output-bindings=existingBinding;myConsumer",
                "activiti.cloud.messaging.connectors.myConsumer.destination=orders",
                "activiti.cloud.messaging.connectors.myConsumer.required-groups=worker"
            )
            .run(context ->
                assertThat(
                    Objects.requireNonNull(context.getEnvironment().getProperty(OUTPUT_BINDINGS_KEY)).split(";")
                ).containsExactlyInAnyOrder("existingBinding", "myConsumer")
            );
    }

    @Test
    void should_notOverrideExplicitDestinationSetByOperator() {
        contextRunner
            .withPropertyValues(
                "activiti.cloud.messaging.connectors.myConsumer.destination=orders",
                "activiti.cloud.messaging.connectors.myConsumer.required-groups=worker",
                "spring.cloud.stream.bindings.myConsumer.destination=operator-orders"
            )
            .run(context ->
                assertThat(
                    context.getEnvironment().getProperty("spring.cloud.stream.bindings.myConsumer.destination")
                ).isEqualTo("operator-orders")
            );
    }

    private String propertySourceName() {
        return ConnectorOutputBindingEnvironmentPostProcessor.class.getSimpleName();
    }
}
