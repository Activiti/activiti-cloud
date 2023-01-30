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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
public class ExampleConnectorIT {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        output.clear();
    }

    @Test
    public void accept_ShouldSendIntegrationResult() throws Exception {
        //given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setProcessInstanceId("10");
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName("myApp");
        integrationRequest.setAppName("myAppName");
        integrationRequest.setAppVersion("1.0");
        integrationRequest.setServiceType("RUNTIME_BUNDLE");
        integrationRequest.setServiceVersion("1.0");

        byte[] payload = objectMapper.writeValueAsBytes(integrationRequest);

        Message<?> message = MessageBuilder.withPayload(payload).build();

        //when
        input.send(message, ExampleConnectorChannels.EXAMPLE_CONNECTOR_CONSUMER);

        //then
        Message<?> outputMessage = output.receive(10000, "integrationResult_myApp");
        assertThat(outputMessage).isNotNull();
        IntegrationResult integrationResult = objectMapper.readValue(
            (byte[]) outputMessage.getPayload(),
            IntegrationResultImpl.class
        );
        assertThat(integrationResult.getIntegrationContext().getOutBoundVariables())
            .containsEntry("var1", "ExampleConnector was called for instance 10");
    }
}
