/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.config.BindingServiceProperties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "activiti.cloud.application.name=foo",
    "spring.application.name=bar",
    "POD_NAMESPACE=baz",
    "ENV_NAME=quix",

    "activiti.cloud.messaging.destination-prefix=${ENV_NAME}.${POD_NAMESPACE}",
    "activiti.cloud.messaging.destination-separator=.",
    "activiti.cloud.messaging.destination-override-enabled=true",

    "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
    "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",

    "activiti.cloud.messaging.destinations.engineEvents.bindings=auditProducer,auditConsumer,queryConsumer",
    "activiti.cloud.messaging.destinations.engineEvents.name=engine-events",

    "activiti.cloud.messaging.destinations.[camel-connector.INVOKE].bindings=camelConnectorConsumer,camel-connector.INVOKE",
    "activiti.cloud.messaging.destinations.[camel-connector.INVOKE].name=${ACT_CAMEL_CONNECTOR_INVOKE_DEST:camel_connector_invoke}",

    "activiti.cloud.messaging.destinations.commandConsumer.bindings=commandConsumer",
    "activiti.cloud.messaging.destinations.commandConsumer.name=command-consumer",
    "activiti.cloud.messaging.destinations.commandConsumer.scope=${activiti.cloud.application.name}",

    "activiti.cloud.messaging.destinations.asyncExecutorJobs.bindings=asyncExecutorJobsInput,asyncExecutorJobsOutput",
    "activiti.cloud.messaging.destinations.asyncExecutorJobs.scope=${activiti.cloud.application.name}",

    "activiti.cloud.messaging.destinations.myCmResults.bindings=commandResults",
    "activiti.cloud.messaging.destinations.myCmResults.name=command-results",
    "activiti.cloud.messaging.destinations.myCmResults.scope=${activiti.cloud.application.name}",
    "activiti.cloud.messaging.destinations.myCmResults.prefix=bar",
    "activiti.cloud.messaging.destinations.myCmResults.separator=_"})
public class ActivitiMessagingDestinationsEnvironmentPostProcessorTests {

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Test
    public void testBindingServicePropertiesDefaults() {
        assertThat(bindingServiceProperties.getBindingProperties("commandConsumer")
                                           .getDestination())
            .isEqualTo("quix.baz.command-consumer.foo");
        assertThat(bindingServiceProperties.getBindingProperties("commandConsumer")
                                           .getGroup())
            .isEqualTo("bar");
    }

    @Test
    public void testBindingServicePropertiesCustomValues() {
        assertThat(bindingServiceProperties.getBindingProperties("commandResults")
                                           .getDestination())
            .isEqualTo("bar_command-results_foo");
    }

    @Test
    public void testBindingServicePropertiesWithMultipleBindings() {
        assertThat(bindingServiceProperties.getBindingProperties("auditProducer")
                                           .getDestination())
            .isEqualTo("quix.baz.engine-events");

        assertThat(bindingServiceProperties.getBindingProperties("auditConsumer")
                                           .getDestination())
            .isEqualTo("quix.baz.engine-events");

        assertThat(bindingServiceProperties.getBindingProperties("queryConsumer")
                                           .getDestination())
            .isEqualTo("quix.baz.engine-events");
    }

    @Test
    public void testBindingServicePropertiesWithConnectorDestinationOverride() {
        assertThat(bindingServiceProperties.getBindingProperties("camelConnectorConsumer")
                                           .getDestination())
            .isEqualTo("quix.baz.camel_connector_invoke");

        assertThat(bindingServiceProperties.getBindingProperties("camel-connector.INVOKE")
                                           .getDestination())
            .isEqualTo("quix.baz.camel_connector_invoke");
    }

    @Test
    public void testBindingServicePropertiesWithAsyncExecutorJobsOverride() {
        assertThat(bindingServiceProperties.getBindingProperties("asyncExecutorJobsInput")
                                           .getDestination())
            .isEqualTo("quix.baz.asyncExecutorJobs.foo");

        assertThat(bindingServiceProperties.getBindingProperties("asyncExecutorJobsOutput")
                                           .getDestination())
            .isEqualTo("quix.baz.asyncExecutorJobs.foo");
    }
}
