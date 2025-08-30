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
package org.activiti.cloud.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.function-router.enabled=true" })
public class CloudConnectorAppFunctionRouterIT extends CloudConnectorAppIT {

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {}

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings()).doesNotContainKeys("functionRouterInput").isNotEmpty();
    }

    @Test
    void functionRouter() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.isEnabled()).isTrue();

        assertThat(functionRouter.getFunctionRoutes()).isEmpty();
        assertThat(functionRouter.destinations()).isEmpty();
        assertThat(functionRouter.registrations()).isEmpty();
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

        assertThat(environment.getProperty("activiti.cloud.messaging.function-router.group", String.class)).isNull();
    }
}
