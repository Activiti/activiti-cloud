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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {"activiti.cloud.messaging.destination-separator=."})
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class EngineConfigurationIT {

    @Autowired
    private SpringProcessEngineConfiguration configuration;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

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
        BindingProperties auditProducer = bindingServiceProperties.getBindingProperties("auditProducer");

        //then
        assertThat(auditProducer.getDestination())
            .as("should have required groups set for audit producer")
            .isEqualTo("engineEvents");

        assertThat(auditProducer.getProducer().getRequiredGroups())
            .as("should have required groups set for audit producer")
            .isEqualTo(new String[] {"query", "audit"});
    }

    @Test
    public void shouldHaveChannelBindingsSetForMessageEvents() {
        //when
        BindingProperties messageEventsOutput = bindingServiceProperties.getBindingProperties("messageEventsOutput");

        //then
        assertThat(messageEventsOutput.getDestination()).isEqualTo("messageEvents.activiti-app");
        assertThat(messageEventsOutput.getProducer().getRequiredGroups()).isEqualTo(new String[] {"messages"});
    }

    @Test
    public void shouldHaveChannelBindingsSetForCommandEndpoint() {
        //when
        BindingProperties commandConsumer = bindingServiceProperties.getBindingProperties("commandConsumer");
        BindingProperties commandResults = bindingServiceProperties.getBindingProperties("commandResults");

        //then
        assertThat(commandConsumer.getDestination()).isEqualTo("commandConsumer.activiti-app");
        assertThat(commandConsumer.getGroup()).isEqualTo("my-activiti-rb-app");
        assertThat(commandResults.getDestination()).isEqualTo("commandResults.activiti-app");
    }

    @Test
    public void shouldHaveChannelBindingsSetForSignalEvents() {
        //when
        BindingProperties signalProducer = bindingServiceProperties.getBindingProperties("signalProducer");
        BindingProperties signalConsumer = bindingServiceProperties.getBindingProperties("signalConsumer");

        //then
        assertThat(signalProducer.getDestination()).isEqualTo("signalEvent");
        assertThat(signalProducer.getProducer().getRequiredGroups()).isEqualTo(new String[] {"my-activiti-rb-app"});
        assertThat(signalConsumer.getDestination()).isEqualTo("signalEvent");
        assertThat(signalConsumer.getGroup()).isEqualTo("my-activiti-rb-app");
    }

    @Test
    public void shouldHaveChannelBindingsSetForCloudConnectors() {
        //when
        BindingProperties integrationResultsConsumer = bindingServiceProperties.getBindingProperties("integrationResultsConsumer");
        BindingProperties integrationErrorsConsumer = bindingServiceProperties.getBindingProperties("integrationErrorsConsumer");

        //then
        assertThat(integrationResultsConsumer.getDestination()).isEqualTo("integrationResult.my-activiti-rb-app");
        assertThat(integrationResultsConsumer.getGroup()).isEqualTo("my-activiti-rb-app");
        assertThat(integrationErrorsConsumer.getDestination()).isEqualTo("integrationError.my-activiti-rb-app");
        assertThat(integrationErrorsConsumer.getGroup()).isEqualTo("my-activiti-rb-app");
    }

    @Test
    public void shouldHaveChannelBindingsSetForAsyncJobExecutor() {
        //when
        BindingProperties asyncExecutorJobsInput = bindingServiceProperties.getBindingProperties("asyncExecutorJobsInput");
        BindingProperties asyncExecutorJobsOutput = bindingServiceProperties.getBindingProperties("asyncExecutorJobsOutput");

        //then
        assertThat(asyncExecutorJobsInput.getDestination()).isEqualTo("asyncExecutorJobs.activiti-app");
        assertThat(asyncExecutorJobsInput.getGroup()).isEqualTo("my-activiti-rb-app");
        assertThat(asyncExecutorJobsOutput.getDestination()).isEqualTo("asyncExecutorJobs.activiti-app");
    }

}
