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
        "activiti.cloud.messaging.destination-transformers-enabled=false",
        "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
        "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",
        "spring.cloud.stream.bindings.messageConnectorOutput.destination=commandConsumer",
        "spring.cloud.stream.bindings.messageConnectorOutput.group=${spring.application.name}",
        "spring.cloud.stream.bindings.auditProducer.destination=engineEvents",
        "spring.cloud.stream.bindings.auditConsumer.destination=engineEvents",
        "spring.cloud.stream.bindings.queryConsumer.destination=engineEvents",
        "spring.cloud.stream.bindings.[camel-connector.INVOKE].destination=camel-connector.INVOKE",
        "spring.cloud.stream.bindings.camelConnectorConsumer.destination=camel-connector.INVOKE",
        "activiti.cloud.messaging.destinations.[camel-connector.INVOKE].name=${ACT_CAMEL_CONNECTOR_INVOKE_DEST:camel_connector_invoke}",
        "spring.cloud.stream.bindings.asyncExecutorJobsInput.destination=asyncExecutorJobs",
        "spring.cloud.stream.bindings.asyncExecutorJobsOutput.destination=asyncExecutorJobs",
        "spring.cloud.stream.bindings.messageEventsOutput.destination=messageEvents",
        "spring.cloud.stream.bindings.messageConnectorInput.destination=messageEvents",
        "spring.cloud.stream.bindings.commandResults.destination=commandResults",
        "activiti.cloud.messaging.destinations.commandResults.prefix=bar",
        "activiti.cloud.messaging.destinations.commandResults.separator=_"
    }
)
public class ActivitiMessagingDestinationsEnvironmentPostProcessorDisabledIT {

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Test
    public void testBindingServicePropertiesDefaults() {
        assertThat(bindingServiceProperties.getBindingProperties("commandConsumer").getDestination())
            .isEqualTo("commandConsumer_foo");
        assertThat(bindingServiceProperties.getBindingProperties("messageConnectorOutput").getDestination())
            .isEqualTo("commandConsumer_foo");
        assertThat(bindingServiceProperties.getBindingProperties("commandConsumer").getGroup()).isEqualTo("bar");
    }

    @Test
    public void testBindingServicePropertiesCustomValues() {
        assertThat(bindingServiceProperties.getBindingProperties("commandResults").getDestination())
            .isEqualTo("bar_commandResults_foo");
    }

    @Test
    public void testBindingServicePropertiesWithMultipleBindings() {
        assertThat(bindingServiceProperties.getBindingProperties("auditProducer").getDestination())
            .isEqualTo("engineEvents");

        assertThat(bindingServiceProperties.getBindingProperties("auditConsumer").getDestination())
            .isEqualTo("engineEvents");

        assertThat(bindingServiceProperties.getBindingProperties("queryConsumer").getDestination())
            .isEqualTo("engineEvents");
    }

    @Test
    public void testBindingServicePropertiesWithConnectorDestinationOverride() {
        assertThat(bindingServiceProperties.getBindingProperties("camelConnectorConsumer").getDestination())
            .isEqualTo("camel_connector_invoke");

        assertThat(bindingServiceProperties.getBindingProperties("camel-connector.INVOKE").getDestination())
            .isEqualTo("camel_connector_invoke");
    }

    @Test
    public void testBindingServicePropertiesWithAsyncExecutorJobsOverride() {
        assertThat(bindingServiceProperties.getBindingProperties("asyncExecutorJobsInput").getDestination())
            .isEqualTo("asyncExecutorJobs_foo");

        assertThat(bindingServiceProperties.getBindingProperties("asyncExecutorJobsOutput").getDestination())
            .isEqualTo("asyncExecutorJobs_foo");
    }

    @Test
    public void testBindingServicePropertiesWithMessageEventsOverrides() {
        assertThat(bindingServiceProperties.getBindingProperties("messageEventsOutput").getDestination())
            .isEqualTo("messageEvents_foo");

        assertThat(bindingServiceProperties.getBindingProperties("messageConnectorInput").getDestination())
            .isEqualTo("messageEvents_foo");
    }
}
