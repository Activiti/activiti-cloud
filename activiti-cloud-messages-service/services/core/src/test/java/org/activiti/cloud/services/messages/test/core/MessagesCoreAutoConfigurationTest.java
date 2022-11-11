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
package org.activiti.cloud.services.messages.test.core;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.dsl.IntegrationFlow;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "activiti.cloud.application.name=my-activiti-rb-app")
public class MessagesCoreAutoConfigurationTest {

    @Autowired
    private IntegrationFlow messageConnectorIntegrationFlow;

    @Autowired
    private ApplicationContext applicationContext;

    @SpringBootApplication
    static class Application {

    }

    @Test
    public void contextLoads() {
        assertThat(messageConnectorIntegrationFlow).isNotNull();

        assertProperty("spring.cloud.stream.bindings.messageConnectorInput-in-0.destination").isEqualTo(
                "messageEvents");
        assertProperty("spring.cloud.stream.bindings.messageConnectorInput-out-0.destination").isEqualTo(
                "commandConsumer"); // TODO: check if this property is correct
//        assertProperty("spring.cloud.stream.bindings.messageConnectorOutput-out-0.destination").isEqualTo(
//                "commandConsumer");
    }

    private AbstractStringAssert<?> assertProperty(String name) {
        return assertThat(getProperty(name));
    }

    private String getProperty(String name) {
        return applicationContext.getEnvironment()
                                 .getProperty(name);
    }
}
