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
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.common.messaging.functional.ConditionalFunctionBinding;
import org.activiti.cloud.connectors.starter.channels.CloudConnectorConsumerChannels;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorSender;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.channels.ProcessRuntimeChannels;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.configuration.EnableActivitiCloudConnector;
import org.activiti.cloud.connectors.starter.model.IntegrationErrorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootApplication
@EnableActivitiCloudConnector
public class ActivitiCloudConnectorApp implements CommandLineRunner {

    private static final String CHANNEL_NAME = "notifications";

    @Autowired
    private ProcessRuntimeChannels processRuntimeChannels;

    @Autowired
    private IntegrationResultSender integrationResultSender;

    @Autowired
    private IntegrationErrorSender integrationErrorSender;

    private static final String OTHER_PROCESS_DEF = "MyOtherProcessDef";

    @Autowired
    private ConnectorProperties connectorProperties;

    public static void main(String[] args) {
        SpringApplication.run(ActivitiCloudConnectorApp.class, args);
    }

    @Override
    public void run(String... args) {
        assertThat(processRuntimeChannels.runtimeCmdProducer()).isNotNull();
    }

    @Bean
    @ConditionalFunctionBinding(
        input = CloudConnectorConsumerChannels.INTEGRATION_EVENT_CONSUMER,
        condition = "headers['type']=='Mock'"
    )
    public Consumer<IntegrationRequest> mockTypeIntegrationRequestEvents() {
        return event -> {
            verifyEventAndCreateResults(event);
            Map<String, Object> resultVariables = createResultVariables(event);
            IntegrationResult integrationResultEvent = resultFor(event, connectorProperties)
                .withOutboundVariables(resultVariables)
                .build();
            Message<IntegrationResult> message = MessageBuilder.withPayload(integrationResultEvent).build();

            integrationResultSender.send(message);
        };
    }

    @Bean
    @ConditionalFunctionBinding(
        input = CloudConnectorConsumerChannels.INTEGRATION_EVENT_CONSUMER,
        condition = "headers['type']=='RuntimeException'"
    )
    public Consumer<IntegrationRequest> mockTypeIntegrationRuntimeError() {
        return event -> {
            throw new RuntimeException("Mock RuntimeException");
        };
    }

    @Bean
    @ConditionalFunctionBinding(
        input = CloudConnectorConsumerChannels.INTEGRATION_EVENT_CONSUMER,
        condition = "headers['type']=='Error'"
    )
    public Consumer<IntegrationRequest> mockTypeIntegrationErrorSender() {
        return integrationRequest -> {
            try {
                throw new Error("Mock Error");
            } catch (Error error) {
                Message<IntegrationError> message = IntegrationErrorBuilder
                    .errorFor(integrationRequest, connectorProperties, error)
                    .buildMessage();
                integrationErrorSender.send(message);
            }
        };
    }

    @Bean
    @ConditionalFunctionBinding(
        input = CloudConnectorConsumerChannels.INTEGRATION_EVENT_CONSUMER,
        condition = "headers['type']=='CloudBpmnError'"
    )
    public Consumer<IntegrationRequest> mockTypeIntegrationCloudBpmnErrorSender() {
        return integrationRequest -> {
            try {
                raiseErrorCause("Error code message");
            } catch (Error cause) {
                Message<IntegrationError> message = IntegrationErrorBuilder
                    .errorFor(integrationRequest, connectorProperties, new CloudBpmnError("ERROR_CODE"))
                    .buildMessage();
                integrationErrorSender.send(message);
            }
        };
    }

    @Bean
    @ConditionalFunctionBinding(
        input = CloudConnectorConsumerChannels.INTEGRATION_EVENT_CONSUMER,
        condition = "headers['type']=='CloudBpmnErrorCause'"
    )
    public Consumer<IntegrationRequest> mockTypeIntegrationCloudBpmnErrorRootCauseSender() {
        return integrationRequest -> {
            try {
                raiseErrorCause("Error cause message");
            } catch (Error cause) {
                Message<IntegrationError> message = IntegrationErrorBuilder
                    .errorFor(integrationRequest, connectorProperties, new CloudBpmnError("ERROR_CODE", cause))
                    .buildMessage();
                integrationErrorSender.send(message);
            }
        };
    }

    @Bean
    @ConditionalFunctionBinding(
        input = CloudConnectorConsumerChannels.INTEGRATION_EVENT_CONSUMER,
        condition = "headers['type']=='CloudBpmnErrorMessage'"
    )
    public Consumer<IntegrationRequest> mockTypeIntegrationCloudBpmnErrorMessageSender() {
        return integrationRequest -> {
            try {
                raiseErrorCause("Error code message");
            } catch (Error cause) {
                Message<IntegrationError> message = IntegrationErrorBuilder
                    .errorFor(
                        integrationRequest,
                        connectorProperties,
                        new CloudBpmnError("ERROR_CODE", cause.getMessage())
                    )
                    .buildMessage();
                integrationErrorSender.send(message);
            }
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
        resultVariables.put("var1", integrationRequest.getIntegrationContext().getInBoundVariables().get("var1"));
        resultVariables.put(
            "var2",
            Long.valueOf(integrationRequest.getIntegrationContext().getInBoundVariables().get("var2").toString()) + 1
        );
        return resultVariables;
    }

    public static void raiseErrorCause(String message) {
        throw new Error(message);
    }
}
