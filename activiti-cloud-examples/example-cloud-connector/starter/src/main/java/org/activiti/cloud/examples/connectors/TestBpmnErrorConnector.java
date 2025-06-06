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

package org.activiti.cloud.examples.connectors;

import static org.activiti.cloud.examples.connectors.TestBpmnErrorConnector.TEST_BPMN_ERROR_CONNECTOR_CONSUMER;

import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationErrorBuilder;
import org.springframework.stereotype.Component;

@ConnectorBinding(
    input = ExampleConnectorChannels.EXAMPLE_CONNECTOR,
    condition = "",
    outputHeader = "",
    connectorType = "test-bpmn-error-connector.throwError"
)
@Component(TEST_BPMN_ERROR_CONNECTOR_CONSUMER + "Connector")
public class TestBpmnErrorConnector implements ConsumerConnector<IntegrationRequest> {

    public static final String TEST_BPMN_ERROR_CONNECTOR_CONSUMER = "testBpmnErrorConnectorInput";

    private IntegrationErrorSender integrationErrorSender;
    private ConnectorProperties connectorProperties;

    public TestBpmnErrorConnector(
        IntegrationErrorSender integrationErrorSender,
        ConnectorProperties connectorProperties
    ) {
        this.integrationErrorSender = integrationErrorSender;
        this.connectorProperties = connectorProperties;
    }

    @Override
    public void accept(IntegrationRequest integrationRequest) {
        CloudBpmnError bpmnError = new CloudBpmnError("CLOUD_BPMN_ERROR");
        integrationErrorSender.send(
            IntegrationErrorBuilder.errorFor(integrationRequest, connectorProperties, bpmnError).buildMessage()
        );
    }
}
