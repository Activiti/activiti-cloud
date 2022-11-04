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

import java.util.AbstractMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {"ACT_MESSAGING_DEST_TRANSFORMERS_ENABLED=true",
                              "ACT_MESSAGING_DEST_SEPARATOR=.",
                              "ACT_MESSAGING_DEST_PREFIX=namespace",
                              "ACT_RB_ENG_EVT_DEST=engine-events",
                              "ACT_RB_SIG_EVT_DEST=signal-event",
                              "ACT_RB_CMD_CONSUMER_DEST=command-consumer",
                              "ACT_RB_ASYNC_JOB_EXEC_DEST=async-executor-jobs",
                              "ACT_RB_MSG_EVT_DEST=message-events",
                              "ACT_RB_CMD_RES_DEST=command-results",
                              "ACT_INT_RES_CONSUMER=integration-result",
                              "ACT_INT_ERR_CONSUMER:integration-error"})
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class EngineConfigurationIT {

    @Autowired
    private SpringProcessEngineConfiguration configuration;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Test
    public void shouldConfigureDefaultConnectorBindingProperties() {
        //given

        //when
        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();

        //then
        assertThat(bindings)
            .extractingFromEntries(entry -> new AbstractMap.SimpleEntry<String, String>(entry.getKey(),
                                                                                        entry.getValue()
                                                                                             .getDestination()))
            .contains(entry("mealsConnector", "namespace.mealsconnector"),
                      entry("rest.GET", "namespace.rest.get"),
                      entry("perfromBusinessTask", "namespace.perfrombusinesstask"),
                      entry("anyImplWithoutHandler", "namespace.anyimplwithouthandler"),
                      entry("payment", "namespace.payment"),
                      entry("Constants Connector.constantsActionName", "namespace.constants-connector.constantsactionname"),
                      entry("Variable Mapping Connector.variableMappingActionName", "namespace.variable-mapping-connector.variablemappingactionname"),
                      entry("miCloudConnector", "namespace.micloudconnector"));
    }


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
        BindingProperties auditProducer = bindingServiceProperties.getBindingProperties("auditProducer-out-0");

        //then
        assertThat(auditProducer.getDestination())
            .as("should have required groups set for audit producer")
            .isEqualTo("namespace.engine-events");

        assertThat(auditProducer.getProducer().getRequiredGroups())
            .as("should have required groups set for audit producer")
            .containsExactly("query", "audit");
    }

    @Test
    public void shouldHaveChannelBindingsSetForMessageEvents() {
        //when
        BindingProperties messageEventsOutput = bindingServiceProperties.getBindingProperties(
                "messageEventsOutput-out-0");

        //then
        assertThat(messageEventsOutput.getDestination()).isEqualTo("namespace.message-events.activiti-app");
        assertThat(messageEventsOutput.getProducer().getRequiredGroups()).containsExactly("messages");
    }

    @Test
    public void shouldHaveChannelBindingsSetForCommandEndpoint() {
        //when
        BindingProperties commandConsumer = bindingServiceProperties.getBindingProperties("commandProcessor-in-0");
        BindingProperties commandResults = bindingServiceProperties.getBindingProperties("commandProcessor-out-0");

        //then
        assertThat(commandConsumer.getDestination()).isEqualTo("namespace.command-consumer.activiti-app");
        assertThat(commandConsumer.getGroup()).isEqualTo("my-activiti-rb-app");
        assertThat(commandResults.getDestination()).isEqualTo("namespace.command-results.activiti-app");
    }

    @Test
    public void shouldHaveChannelBindingsSetForSignalEvents() {
        //when
        BindingProperties signalProducer = bindingServiceProperties.getBindingProperties("signalProducer-out-0");
        BindingProperties signalConsumer = bindingServiceProperties.getBindingProperties("signalConsumer-in-0");

        //then
        assertThat(signalProducer.getDestination()).isEqualTo("namespace.signal-event");
        assertThat(signalProducer.getProducer().getRequiredGroups()).containsExactly("my-activiti-rb-app");
        assertThat(signalConsumer.getDestination()).isEqualTo("namespace.signal-event");
        assertThat(signalConsumer.getGroup()).isEqualTo("my-activiti-rb-app");
    }

    @Test
    public void shouldHaveChannelBindingsSetForCloudConnectors() {
        //when
        BindingProperties integrationResultsConsumer = bindingServiceProperties.getBindingProperties(
                "integrationResultsConsumer-in-0");
        BindingProperties integrationErrorsConsumer = bindingServiceProperties.getBindingProperties(
                "integrationErrorsConsumer-in-0");

        //then
        assertThat(integrationResultsConsumer.getDestination()).isEqualTo("namespace.integration-result.my-activiti-rb-app");
        assertThat(integrationResultsConsumer.getGroup()).isEqualTo("my-activiti-rb-app");
        assertThat(integrationErrorsConsumer.getDestination()).isEqualTo("namespace.integration-error.my-activiti-rb-app");
        assertThat(integrationErrorsConsumer.getGroup()).isEqualTo("my-activiti-rb-app");
    }

    @Test
    public void shouldHaveChannelBindingsSetForAsyncJobExecutor() {
        //when
        BindingProperties asyncExecutorJobsInput = bindingServiceProperties.getBindingProperties("asyncExecutorJobsInput");
        BindingProperties asyncExecutorJobsOutput = bindingServiceProperties.getBindingProperties("asyncExecutorJobsOutput");

        //then
        assertThat(asyncExecutorJobsInput.getDestination()).isEqualTo("namespace.async-executor-jobs.activiti-app");
        assertThat(asyncExecutorJobsInput.getGroup()).isEqualTo("my-activiti-rb-app");
        assertThat(asyncExecutorJobsOutput.getDestination()).isEqualTo("namespace.async-executor-jobs.activiti-app");
    }

}
