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
package org.activiti.cloud.services.test.containers;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.RabbitMQContainer;

public class RabbitMQContainerApplicationInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.8.9-management-alpine")
            .withReuse(true)
            .withExposedPorts(5672, 5671, 15672, 15671);

    @Override
    public void initialize(ConfigurableApplicationContext context) {

        if (!rabbitMQContainer.isRunning()) {
            rabbitMQContainer.start();
        }

        TestPropertyValues.of(getContainerProperties()).applyTo(context.getEnvironment());

    }

    public static RabbitMQContainer getContainer() {
        return rabbitMQContainer;
    }

    public static String[] getContainerProperties() {
        return new String[] {
            "spring.rabbitmq.host=" + rabbitMQContainer.getContainerIpAddress(),
            "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpPort()};
    }
}
