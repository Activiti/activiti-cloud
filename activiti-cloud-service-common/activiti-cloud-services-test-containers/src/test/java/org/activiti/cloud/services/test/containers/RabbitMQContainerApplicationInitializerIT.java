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
package org.activiti.cloud.services.test.containers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = { TestContainersApplication.class })
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class })
class RabbitMQContainerApplicationInitializerIT {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        // application context loads successfully
    }

    @Test
    void shouldSetVirtualHostProperty() {
        String virtualHost = environment.getProperty("spring.rabbitmq.virtual-host");
        assertThat(virtualHost)
            .as("spring.rabbitmq.virtual-host should be set to a unique UUID vhost")
            .isNotNull()
            .isNotEmpty()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void shouldSetRabbitMQHostAndPortProperties() {
        assertThat(environment.getProperty("spring.rabbitmq.host"))
            .as("spring.rabbitmq.host should be set")
            .isNotNull()
            .isNotEmpty();
        assertThat(environment.getProperty("spring.rabbitmq.port"))
            .as("spring.rabbitmq.port should be set")
            .isNotNull()
            .isNotEmpty();
    }
}
