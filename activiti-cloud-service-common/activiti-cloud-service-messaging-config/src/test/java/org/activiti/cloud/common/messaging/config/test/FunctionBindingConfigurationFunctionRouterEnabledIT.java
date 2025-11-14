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
package org.activiti.cloud.common.messaging.config.test;

import static org.activiti.cloud.common.messaging.config.AbstractFunctionalBindingConfiguration.getInBinding;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.AUDIT_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.COMMAND_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.QUERY_CONSUMER;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {
        "activiti.cloud.messaging.function-router.enabled=true",
        "activiti.cloud.messaging.function-router.routes.commandConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.auditConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.queryConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.integrationRequests.enabled=true",
    }
)
@Import(FunctionBindingConfigurationFunctionRouterEnabledIT.ApplicationConfig.class)
public class FunctionBindingConfigurationFunctionRouterEnabledIT extends FunctionBindingConfigurationIT {

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Test
    @Override
    void testInputBindingsDefinitions() {
        Assertions.assertThat(context.getBean(COMMAND_CONSUMER, MessageChannel.class)).isNotNull();
        Assertions
            .assertThat(bindingServiceProperties.getInputBindings())
            .doesNotContain(FUNCTION_COMMAND_CONSUMER_NAME);
        Assertions
            .assertThat(streamFunctionProperties.getBindings().get(getInBinding(FUNCTION_COMMAND_CONSUMER_NAME)))
            .isNull();
        Assertions.assertThat(context.getBean(AUDIT_CONSUMER, MessageChannel.class)).isNotNull();
        Assertions.assertThat(bindingServiceProperties.getInputBindings()).doesNotContain(FUNCTION_AUDIT_CONSUMER_NAME);
        Assertions
            .assertThat(streamFunctionProperties.getBindings().get(getInBinding(FUNCTION_AUDIT_CONSUMER_NAME)))
            .isNull();
        Assertions.assertThat(context.getBean(QUERY_CONSUMER, MessageChannel.class)).isNotNull();
        Assertions.assertThat(bindingServiceProperties.getInputBindings()).doesNotContain(FUNCTION_QUERY_CONSUMER_NAME);
        Assertions
            .assertThat(streamFunctionProperties.getBindings().get(getInBinding(FUNCTION_QUERY_CONSUMER_NAME)))
            .isNull();
    }

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties).isNotNull();

        // when
        var bindings = bindingServiceProperties.getBindings();

        // then
        AssertionsForClassTypes
            .assertThat(bindings)
            .asInstanceOf(InstanceOfAssertFactories.map(String.class, BindingProperties.class))
            .containsOnlyKeys(
                "auditProducer",
                "commandResults",
                "integrationResults",
                "functionRouterInput",
                "functionRouterAnonymousInput"
            );
    }
}
