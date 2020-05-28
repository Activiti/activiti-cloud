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
package org.activiti.cloud.starter.messages.test.mongodb;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MongoDBContainer;

public class MongodbApplicationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static MongoDBContainer container = new MongoDBContainer();

    @Override
    public void initialize(ConfigurableApplicationContext context) {

        container.start();

        try {

            TestPropertyValues.of(
                "spring.data.mongodb.uri=mongodb://" + container.getContainerIpAddress() + ":" + container.getFirstMappedPort() + "/test"
            ).applyTo(context.getEnvironment());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
