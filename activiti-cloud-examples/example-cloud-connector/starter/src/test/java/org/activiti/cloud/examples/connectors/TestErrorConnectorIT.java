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
import java.util.UUID;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.model.shared.messages.IntegrationContextMessageHeaders;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
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
public class TestErrorConnectorIT {

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
    public void accept_ShouldThrowAndSendIntegrationError() throws Exception {
        //given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName("myApp");
        integrationRequest.setAppName("myAppName");
        integrationRequest.setAppVersion("1.0");
        integrationRequest.setServiceType("RUNTIME_BUNDLE");
        integrationRequest.setServiceVersion("1.0");

        byte[] payload = objectMapper.writeValueAsBytes(integrationRequest);

        Message<?> message = MessageBuilder
            .withPayload(payload)
            .setHeader(IntegrationContextMessageHeaders.INTEGRATION_CONTEXT_ID, UUID.randomUUID().toString())
            .build();

        //when
        input.send(message, TestErrorConnector.Channels.CHANNEL);

        //then
        Message<?> outputMessage = output.receive(10000, "integrationError_myApp");
        assertThat(outputMessage).isNotNull();
        IntegrationError integrationError = objectMapper.readValue(
            (byte[]) outputMessage.getPayload(),
            IntegrationErrorImpl.class
        );
        assertThat(integrationError)
            .extracting(
                IntegrationError::getErrorClassName,
                IntegrationError::getErrorCode,
                IntegrationError::getErrorMessage
            )
            .contains(RuntimeException.class.getName(), null, "TestErrorConnector");
    }
}
