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
package org.activiti.cloud.connectors.starter;

import static org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder.resultFor;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.connectors.starter.bindings.TestMessageRoutingCallback;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorSender;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.configuration.EnableActivitiCloudConnector;
import org.activiti.cloud.connectors.starter.model.IntegrationErrorBuilder;
import org.activiti.cloud.connectors.starter.test.it.ConnectorsITStreamHandlers;
import org.activiti.cloud.connectors.starter.test.it.RuntimeMockStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootApplication
@EnableActivitiCloudConnector
public class ActivitiCloudConnectorApp implements CommandLineRunner {

    private final IntegrationResultSender integrationResultSender;

    private final IntegrationErrorSender integrationErrorSender;

    @Autowired
    private FunctionRegistry functionRegistry;

    @Autowired
    private ConnectorProperties connectorProperties;

    public ActivitiCloudConnectorApp(IntegrationResultSender integrationResultSender,
                                     IntegrationErrorSender integrationErrorSender) {
        this.integrationResultSender = integrationResultSender;
        this.integrationErrorSender = integrationErrorSender;
    }

    public static void main(String[] args) {
        SpringApplication.run(ActivitiCloudConnectorApp.class,
                              args);
    }

    @Override
    public void run(String... args) throws Exception {
        assertThat(integrationResultSender).isNotNull();
        assertThat(integrationErrorSender).isNotNull();
        Function<?,?> integrationErrorHandler = functionRegistry.lookup("integrationErrorHandler");
        assertThat(integrationErrorHandler).isNotNull();
    }

    @Bean
    public MessageRoutingCallback messageRoutingCallback() {
        return new TestMessageRoutingCallback();
    }

    @Bean(TestMessageRoutingCallback.MOCK_INTEGRATION_REQUEST_EVENT_BINDING)
    public Consumer<IntegrationRequest> mockTypeIntegrationRequestEventsConsumer() {
        return  (IntegrationRequest event) -> {
            verifyEventAndCreateResults(event);
            Map<String, Object> resultVariables = createResultVariables(event);
            IntegrationResult integrationResultEvent = resultFor(event, connectorProperties)
                .withOutboundVariables(resultVariables)
                .build();
            Message<IntegrationResult> message = MessageBuilder.withPayload(integrationResultEvent).build();

            integrationResultSender.send(message);
        };
    }

    @Bean(TestMessageRoutingCallback.MOCK_INTEGRATION_RUNTIME_ERROR_BINDING)
    public Consumer<IntegrationRequest> mockTypeIntegrationRuntimeErrorConsumer() {
        return (IntegrationRequest event) -> {
            throw new RuntimeException("Mock RuntimeException");
        };
    }

    @Bean(TestMessageRoutingCallback.MOCK_INTEGRATION_ERROR_SENDER_BINDING)
    public Consumer<IntegrationRequest> mockTypeIntegrationErrorSenderConsumer() {
        return (IntegrationRequest event) -> {
            try {

                throw new Error("Mock Error");

            } catch (Error error) {
                Message<IntegrationError> message = IntegrationErrorBuilder.errorFor(event,
                        connectorProperties,
                        error)
                    .buildMessage();
                integrationErrorSender.send(message);
            }
        };
    }

    @Bean(TestMessageRoutingCallback.MOCK_INTEGRATION_CLOUD_BPMN_ERROR_SENDER_BINDING)
    public Consumer<IntegrationRequest> mockTypeIntegrationCloudBpmnErrorSenderConsumer() {
        return (IntegrationRequest event) -> {
            try {

                raiseErrorCause("Error code message");

            } catch (Error cause) {
                Message<IntegrationError> message = IntegrationErrorBuilder.errorFor(event,
                        connectorProperties,
                        new CloudBpmnError("ERROR_CODE"))
                    .buildMessage();
                integrationErrorSender.send(message);
            }
        };
    }

    @Bean(TestMessageRoutingCallback.MOCK_INTEGRATION_CLOUD_BPMN_ERROR_ROOT_CAUSE_SENDER_BINDING)
    public Consumer<IntegrationRequest> mockTypeIntegrationCloudBpmnErrorRootCauseSenderConsumer() {
        return (IntegrationRequest event) -> {
            try {

                raiseErrorCause("Error cause message");

            } catch (Error cause) {
                Message<IntegrationError> message = IntegrationErrorBuilder.errorFor(event,
                        connectorProperties,
                        new CloudBpmnError("ERROR_CODE", cause))
                    .buildMessage();
                integrationErrorSender.send(message);
            }
        };
    }

    @Bean(TestMessageRoutingCallback.MOCK_INTEGRATION_CLOUD_BPMN_ERROR_MESSAGE_SENDER_BINDING)
    public Consumer<IntegrationRequest> mockTypeIntegrationCloudBpmnErrorMessageSenderConsumer() {
        return (IntegrationRequest event) -> {
            try {

                raiseErrorCause("Error code message");

            } catch (Error cause) {
                Message<IntegrationError> message = IntegrationErrorBuilder.errorFor(event,
                        connectorProperties,
                        new CloudBpmnError("ERROR_CODE", cause.getMessage()))
                    .buildMessage();
                integrationErrorSender.send(message);
            }
        };
    }

    @Profile(ConnectorsITStreamHandlers.CONNECTOR_IT)
    @Bean(RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
    public Consumer<IntegrationResult> consumeIntegrationResultsMock(ConnectorsITStreamHandlers connectorsITStreamHandlers) {
        return (IntegrationResult event) -> {
            connectorsITStreamHandlers.consumeIntegrationResultsMock(event);
        };
    }

    @Profile(ConnectorsITStreamHandlers.CONNECTOR_IT)
    @Bean(RuntimeMockStreams.INTEGRATION_ERROR_CONSUMER)
    public Consumer<IntegrationError> consumeIntegrationErrorMock(ConnectorsITStreamHandlers connectorsITStreamHandlers) {
        return (IntegrationError event) -> {
            connectorsITStreamHandlers.consumeIntegrationErrorMock(event);
        };
    }

    private void verifyEventAndCreateResults(IntegrationRequest event) {
        assertThat(event.getIntegrationContext().getId()).isNotEmpty();
        assertThat(event).isNotNull();
        assertThat(event.getIntegrationContext().getProcessDefinitionId()).isNotNull();
        assertThat(event.getIntegrationContext().getProcessInstanceId()).isNotNull();
    }

    private Map<String, Object> createResultVariables(IntegrationRequest integrationRequest) {
        Map<String, Object> resultVariables = new HashMap<>();
        resultVariables.put("var1",
                            integrationRequest.getIntegrationContext().getInBoundVariables().get("var1"));
        resultVariables.put("var2",
                            Long.valueOf(integrationRequest.getIntegrationContext().getInBoundVariables().get("var2").toString()) + 1);
        return resultVariables;
    }

    public static void raiseErrorCause(String message) {
        throw new Error(message);
    }

}
