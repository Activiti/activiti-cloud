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
package org.activiti.cloud.starter.tests.conf;

import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.rb.behavior.CloudActivityBehaviorFactory;
import org.activiti.runtime.api.impl.MappingAwareActivityBehaviorFactory;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class EngineConfigurationIT {

    @Autowired
    private SpringProcessEngineConfiguration configuration;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void shouldUseCloudActivityBehaviorFactory() {
        assertThat(configuration.getActivityBehaviorFactory())
            .isInstanceOf(MappingAwareActivityBehaviorFactory.class)
            .isInstanceOf(CloudActivityBehaviorFactory.class);

        assertThat(configuration.getBpmnParser().getActivityBehaviorFactory())
            .isInstanceOf(MappingAwareActivityBehaviorFactory.class)
            .isInstanceOf(CloudActivityBehaviorFactory.class);
    }

    @Test
    public void shouldHaveRequiredGroupsSetForAuditProducer() {
        //when
        assertProperty("spring.cloud.stream.bindings.auditProducer.producer.required-groups")
            .as("should have required groups set for audit producer")
            .isEqualTo("query,audit");

        assertProperty("spring.cloud.stream.bindings.auditProducer.destination")
            .as("should have destination set for audit producer")
            .isEqualTo("engineEvents");
    }

    @Test
    public void shouldHaveChannelBindingsSetForMessageEvents() {
        //when
        assertProperty("spring.cloud.stream.bindings.messageEvents.destination").isEqualTo("messageEvents_activiti-app");
        assertProperty("spring.cloud.stream.bindings.messageEvents.producer.required-groups").isEqualTo("messages");
    }

    @Test
    public void shouldHaveChannelBindingsSetForCommandEndpoint() {
        //when
        assertProperty("spring.cloud.stream.bindings.commandConsumer.destination").isEqualTo("commandConsumer_activiti-app");
        assertProperty("spring.cloud.stream.bindings.commandConsumer.group").isEqualTo("my-activiti-rb-app");
        assertProperty("spring.cloud.stream.bindings.commandResults.destination").isEqualTo("commandResults_activiti-app");

    }

    @Test
    public void shouldHaveChannelBindingsSetForSignalEvents() {
        //when
        assertProperty("spring.cloud.stream.bindings.signalProducer.destination").isEqualTo("signalEvent");
        assertProperty("spring.cloud.stream.bindings.signalProducer.producer.required-groups").isEqualTo("my-activiti-rb-app");
        assertProperty("spring.cloud.stream.bindings.signalConsumer.destination").isEqualTo("signalEvent");
        assertProperty("spring.cloud.stream.bindings.signalConsumer.group").isEqualTo("my-activiti-rb-app");
    }

    @Test
    public void shouldHaveChannelBindingsSetForCloudConnectors() {
        //when
        assertProperty("spring.cloud.stream.bindings.integrationResultsConsumer.destination").isEqualTo("integrationResult_activiti-app");
        assertProperty("spring.cloud.stream.bindings.integrationResultsConsumer.group").isEqualTo("my-activiti-rb-app");
        assertProperty("spring.cloud.stream.bindings.integrationErrorsConsumer.destination").isEqualTo("integrationError_activiti-app");
        assertProperty("spring.cloud.stream.bindings.integrationErrorsConsumer.group").isEqualTo("my-activiti-rb-app");
    }

    private AbstractStringAssert<?> assertProperty(String name) {
        return assertThat(getProperty(name));
    }

    private String getProperty(String name) {
        return applicationContext.getEnvironment()
                                 .getProperty(name);
    }
}
