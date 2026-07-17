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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

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
                "activiti.cloud.messaging.connectors.myConsumer.binding-key=myConsumer",
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
                "activiti.cloud.messaging.connectors.fanout.binding-key=fanout",
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
                "activiti.cloud.messaging.connectors.plain.binding-key=plain",
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
                "activiti.cloud.messaging.connectors.first.binding-key=first",
                "activiti.cloud.messaging.connectors.first.destination=orders-create",
                "activiti.cloud.messaging.connectors.first.required-groups=worker",
                "activiti.cloud.messaging.connectors.second.binding-key=second",
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
                "activiti.cloud.messaging.connectors.myConsumer.binding-key=myConsumer",
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
                "activiti.cloud.messaging.connectors.myConsumer.binding-key=myConsumer",
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

    @Test
    void should_useBindingKey_asBindingName() {
        contextRunner
            .withPropertyValues(
                "activiti.cloud.messaging.connectors.someMapKey.binding-key=myBinding",
                "activiti.cloud.messaging.connectors.someMapKey.destination=orders",
                "activiti.cloud.messaging.connectors.someMapKey.required-groups=worker"
            )
            .run(context -> {
                ConfigurableEnvironment environment = context.getEnvironment();
                assertThat(environment.getProperty("spring.cloud.stream.bindings.myBinding.destination")).isEqualTo(
                    "orders"
                );
                assertThat(
                    environment.getProperty("spring.cloud.stream.bindings.myBinding.producer.required-groups")
                ).isEqualTo("worker");
                assertThat(environment.getProperty("spring.cloud.stream.bindings.someMapKey.destination")).isNull();
                assertThat(environment.getProperty(OUTPUT_BINDINGS_KEY)).isEqualTo("myBinding");
            });
    }

    @Test
    void should_emitRawBracketedPropertyKeys_when_bindingKeyIsBracketed() {
        contextRunner
            .withPropertyValues(
                "activiti.cloud.messaging.connectors.idp.binding-key=[idp-connector-tmihg.CLASSIFICATION]",
                "activiti.cloud.messaging.connectors.idp.destination=orders-update",
                "activiti.cloud.messaging.connectors.idp.required-groups=worker-a,worker-b",
                "activiti.cloud.messaging.connectors.idp.queue-name-group-only=true"
            )
            .run(context -> {
                PropertySource<?> source = context.getEnvironment().getPropertySources().get(propertySourceName());
                assertThat(source).isNotNull();
                assertThat(
                    source.getProperty("spring.cloud.stream.bindings.[idp-connector-tmihg.CLASSIFICATION].destination")
                ).isEqualTo("orders-update");
                assertThat(
                    source.getProperty(
                        "spring.cloud.stream.bindings.[idp-connector-tmihg.CLASSIFICATION].producer.required-groups"
                    )
                ).isEqualTo("worker-a,worker-b");
                assertThat(
                    source.getProperty(
                        "spring.cloud.stream.rabbit.bindings.[idp-connector-tmihg.CLASSIFICATION].producer.queue-name-group-only"
                    )
                ).isEqualTo("true");
                assertThat(source.getProperty(OUTPUT_BINDINGS_KEY)).isEqualTo("[idp-connector-tmihg.CLASSIFICATION]");
            });
    }

    @Test
    void should_reconstructSingleBindingName_when_bindingKeyIsBracketed() {
        contextRunner
            .withPropertyValues(
                "activiti.cloud.messaging.connectors.idp.binding-key=[idp-connector-tmihg.CLASSIFICATION]",
                "activiti.cloud.messaging.connectors.idp.destination=orders-update"
            )
            .run(context -> {
                Map<String, Object> bindings = Binder.get(context.getEnvironment())
                    .bind("spring.cloud.stream.bindings", Bindable.mapOf(String.class, Object.class))
                    .orElseGet(Collections::emptyMap);
                assertThat(bindings).containsKey("idp-connector-tmihg.CLASSIFICATION");
            });
    }

    @Test
    void should_failFast_when_bindingKeyMissing() {
        contextRunner
            .withPropertyValues(
                "activiti.cloud.messaging.connectors.someMapKey.destination=orders-update",
                "activiti.cloud.messaging.connectors.someMapKey.required-groups=worker"
            )
            .run(context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("binding-key is required")
            );
    }

    private String propertySourceName() {
        return ConnectorOutputBindingEnvironmentPostProcessor.class.getSimpleName();
    }
}
