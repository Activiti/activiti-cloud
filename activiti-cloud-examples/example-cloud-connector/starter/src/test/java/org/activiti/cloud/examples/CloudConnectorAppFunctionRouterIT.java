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
package org.activiti.cloud.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.function-router.enabled=true" })
public class CloudConnectorAppFunctionRouterIT extends CloudConnectorAppIT {

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings()).isNotEmpty().containsOnlyKeys("functionRouterInput");
    }

    @Test
    void functionRouterEnabled() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.isEnabled()).isTrue();
    }

    @Test
    void functionRouterRoutes() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.getFunctionRoutes()).isNotEmpty().containsOnly("example-connector");
    }

    @Test
    void functionRouterDestinations() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.destinations())
            .isNotEmpty()
            .containsOnly(
                Map.entry(
                    "example-connector",
                    "restconnector.POST,restConnector.GET,test-bpmn-error-connector.throwError,test-error-connector.throwError,miCloudConnector,headers.GET,Movies.getMovieDesc,ExampleConnector"
                )
            );
    }

    @Test
    void functionRouterRegistrations() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.registrations())
            .isNotEmpty()
            .containsOnly(
                Map.entry("ExampleConnector", List.of("exampleConnectorConsumerConnector_registration")),
                Map.entry("Movies.getMovieDesc", List.of("moviesDescriptionConsumerConnector_registration")),
                Map.entry("headers.GET", List.of("headersConnectorConsumerConnector_registration")),
                Map.entry("miCloudConnector", List.of("miCloudConnectorInputConnector_registration")),
                Map.entry("restconnector.POST", List.of("restConnectorPOST_registration")),
                Map.entry(
                    "test-bpmn-error-connector.throwError",
                    List.of("testBpmnErrorConnectorInputConnector_registration")
                ),
                Map.entry("test-error-connector.throwError", List.of("testErrorConnectorInputConnector_registration"))
            );
    }

    @Test
    void environment() {
        assertThat(
            environment.getProperty(
                "spring.cloud.stream.rabbit.bindings.functionRouterInput.consumer.queue-name-group-only",
                Boolean.class
            )
        )
            .isTrue();

        assertThat(environment.getProperty("activiti.cloud.messaging.function-router.group", String.class))
            .isEqualTo("processing-connector");
    }
}
