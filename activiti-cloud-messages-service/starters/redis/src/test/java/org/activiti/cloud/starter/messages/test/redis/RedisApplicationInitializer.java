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
package org.activiti.cloud.starter.messages.test.redis;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

public class RedisApplicationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static GenericContainer container = new GenericContainer("redis").withReuse(false).withExposedPorts(6379);

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        container.start();

        TestPropertyValues
            .of(
                "spring.data.redis.host=" + container.getContainerIpAddress(),
                "spring.data.redis.port=" + container.getFirstMappedPort()
            )
            .applyTo(context.getEnvironment());
    }
}
