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

import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "activiti.cloud.application.name=foo",
    "spring.application.name=bar",
    "activiti.cloud.messaging.destination-prefix=baz",
    "activiti.cloud.messaging.destination-separator=.",
    "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
    "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",
    "activiti.cloud.messaging.destinations.engineEvents.bindings=auditProducer,auditConsumer,queryConsumer",
    "activiti.cloud.messaging.destinations.engineEvents.scope=engineEvents",
    "activiti.cloud.messaging.destinations.commandConsumer.scope=commandConsumer_${activiti.cloud.application.name}",
    "activiti.cloud.messaging.destinations.myCmResults.bindings=commandResults",
    "activiti.cloud.messaging.destinations.myCmResults.scope=commandResults_${activiti.cloud.application.name}",
    "activiti.cloud.messaging.destinations.myCmResults.prefix=bar",
    "activiti.cloud.messaging.destinations.myCmResults.separator=-"})
public class ActivitiMessagingDestinationsEnvironmentPostProcessorTests {

    @Autowired
    private ActivitiCloudMessagingProperties properties;

    @Autowired
    private Environment env;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Test
    public void testBindingServicePropertiesDefaults() {
        assertThat(bindingServiceProperties.getBindingProperties("commandConsumer")
                                           .getDestination())
            .isEqualTo("baz.commandConsumer_foo");
        assertThat(bindingServiceProperties.getBindingProperties("commandConsumer")
                                           .getGroup())
            .isEqualTo("bar");
    }

    @Test
    public void testBindingServicePropertiesCustomValues() {
        assertThat(bindingServiceProperties.getBindingProperties("commandResults")
                                           .getDestination())
            .isEqualTo("bar-commandResults_foo");
    }

    @Test
    public void testBindingServicePropertiesWithMultipleBindings() {
        assertThat(bindingServiceProperties.getBindingProperties("auditProducer")
                                           .getDestination())
            .isEqualTo("baz.engineEvents");

        assertThat(bindingServiceProperties.getBindingProperties("auditConsumer")
                                           .getDestination())
            .isEqualTo("baz.engineEvents");

        assertThat(bindingServiceProperties.getBindingProperties("queryConsumer")
                                           .getDestination())
            .isEqualTo("baz.engineEvents");
    }
}
