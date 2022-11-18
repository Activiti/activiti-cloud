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

package org.activiti.cloud.common.messaging.config.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.config.BindingServiceProperties;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=foo",
        "spring.application.name=bar",
        "POD_NAMESPACE=baz",
        "ENV_NAME=quix",
        "DEST_SEPARATOR=.",
        "activiti.cloud.messaging.destination-separator=${DEST_SEPARATOR}",
        "activiti.cloud.messaging.destination-prefix=${ENV_NAME}${DEST_SEPARATOR}${POD_NAMESPACE}",
        "activiti.cloud.messaging.destination-transformers-enabled=true",
        "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
        "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",
        "spring.cloud.stream.bindings.messageConnectorOutput.destination=commandConsumer",
        "spring.cloud.stream.bindings.messageConnectorOutput.group=${spring.application.name}",
        "spring.cloud.stream.bindings.auditProducer.destination=engineEvents",
        "spring.cloud.stream.bindings.auditConsumer.destination=engineEvents",
        "spring.cloud.stream.bindings.queryConsumer.destination=engineEvents",
        // override destination name
        "activiti.cloud.messaging.destinations.engineEvents.name=engine-events",
        "spring.cloud.stream.bindings.[camel-connector.INVOKE].destination=camel-connector.INVOKE",
        "spring.cloud.stream.bindings.camelConnectorConsumer.destination=camel-connector.INVOKE",
        "activiti.cloud.messaging.destinations.[camel-connector.INVOKE].name=${ACT_CAMEL_CONNECTOR_INVOKE_DEST:camel_connector_invoke}",
        "spring.cloud.stream.bindings.asyncExecutorJobsInput.destination=asyncExecutorJobs",
        "spring.cloud.stream.bindings.asyncExecutorJobsOutput.destination=asyncExecutorJobs",
        "spring.cloud.stream.bindings.messageEventsOutput.destination=messageEvents",
        "spring.cloud.stream.bindings.messageConnectorInput.destination=messageEvents",
        "activiti.cloud.messaging.destinations.messageEvents.name=message-events",
        "spring.cloud.stream.bindings.commandResults.destination=commandResults",
        "activiti.cloud.messaging.destinations.commandResults.name=command-results",
        "activiti.cloud.messaging.destinations.commandResults.prefix=bar",
        "activiti.cloud.messaging.destinations.commandResults.separator=_"
    }
)
public class ActivitiMessagingDestinationsEnvironmentPostProcessorIT {

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Test
    public void testBindingServicePropertiesDefaults() {
        assertThat(bindingServiceProperties.getBindingProperties("commandConsumer").getDestination())
            .isEqualTo("quix.baz.commandconsumer.foo");
        assertThat(bindingServiceProperties.getBindingProperties("messageConnectorOutput").getDestination())
            .isEqualTo("quix.baz.commandconsumer.foo");
        assertThat(bindingServiceProperties.getBindingProperties("commandConsumer").getGroup()).isEqualTo("bar");
    }

    @Test
    public void testBindingServicePropertiesCustomValues() {
        assertThat(bindingServiceProperties.getBindingProperties("commandResults").getDestination())
            .isEqualTo("bar_command-results_foo");
    }

    @Test
    public void testBindingServicePropertiesWithMultipleBindings() {
        assertThat(bindingServiceProperties.getBindingProperties("auditProducer").getDestination())
            .isEqualTo("quix.baz.engine-events");

        assertThat(bindingServiceProperties.getBindingProperties("auditConsumer").getDestination())
            .isEqualTo("quix.baz.engine-events");

        assertThat(bindingServiceProperties.getBindingProperties("queryConsumer").getDestination())
            .isEqualTo("quix.baz.engine-events");
    }

    @Test
    public void testBindingServicePropertiesWithConnectorDestinationOverride() {
        assertThat(bindingServiceProperties.getBindingProperties("camelConnectorConsumer").getDestination())
            .isEqualTo("quix.baz.camel_connector_invoke");

        assertThat(bindingServiceProperties.getBindingProperties("camel-connector.INVOKE").getDestination())
            .isEqualTo("quix.baz.camel_connector_invoke");
    }

    @Test
    public void testBindingServicePropertiesWithAsyncExecutorJobsOverride() {
        assertThat(bindingServiceProperties.getBindingProperties("asyncExecutorJobsInput").getDestination())
            .isEqualTo("quix.baz.asyncexecutorjobs.foo");

        assertThat(bindingServiceProperties.getBindingProperties("asyncExecutorJobsOutput").getDestination())
            .isEqualTo("quix.baz.asyncexecutorjobs.foo");
    }

    @Test
    public void testBindingServicePropertiesWithMessageEventsOverrides() {
        assertThat(bindingServiceProperties.getBindingProperties("messageEventsOutput").getDestination())
            .isEqualTo("quix.baz.message-events.foo");

        assertThat(bindingServiceProperties.getBindingProperties("messageConnectorInput").getDestination())
            .isEqualTo("quix.baz.message-events.foo");
    }
}
