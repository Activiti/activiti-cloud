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
package org.activiti.cloud.common.messaging.config.test;

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_ROUTER_ANONYMOUS_INPUT;
import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_ROUTER_INPUT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {
        "activiti.cloud.messaging.function-router.enabled=true",
        "activiti.cloud.messaging.function-router.routes.commandConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.auditConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.queryConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.integrationRequests.enabled=true",
        "activiti.cloud.messaging.function-router.request-timeout=10s",
    }
)
@Import(ConnectorConfigurationFunctionRouterEnabledIT.ApplicationConfig.class)
public class ConnectorConfigurationFunctionRouterEnabledIT extends ConnectorConfigurationIT {

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @Autowired
    private Function<
        ConnectorBinding,
        Optional<SimpleFunctionRegistry.FunctionInvocationWrapper>
    > connectorErrorHandlerDefinitionResolver;

    @Test
    @Override
    void defaultErrorHandlerDefinition() {
        AssertionsForClassTypes.assertThat(bindingServiceProperties.getBindingProperties(FUNCTION_ROUTER_INPUT))
            .extracting(BindingProperties::getErrorHandlerDefinition)
            .isEqualTo("functionRouterErrorMessageHandler");

        AssertionsForClassTypes.assertThat(
            bindingServiceProperties.getBindingProperties(FUNCTION_ROUTER_ANONYMOUS_INPUT)
        )
            .extracting(BindingProperties::getErrorHandlerDefinition)
            .isEqualTo("functionRouterErrorMessageHandler");
    }

    @Test
    void functionRouterBindingsConnectorErrorHandlerDefinition() {
        assertThat(messagingProperties.getFunctionRouter().bindings().get("auditConsumer"))
            .extracting(BindingProperties::getErrorHandlerDefinition)
            .isEqualTo("myErrorHandler");
    }

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties).isNotNull();

        // when
        var bindings = bindingServiceProperties.getBindings();

        // then
        AssertionsForClassTypes.assertThat(bindings)
            .asInstanceOf(InstanceOfAssertFactories.map(String.class, BindingProperties.class))
            .containsOnlyKeys(
                "functionRouterInput",
                "functionRouterAnonymousInput",
                "commandResults",
                "integrationResults",
                "auditProducer",
                "auditProducerIncidents",
                "script.EXECUTE"
            );
    }
}
