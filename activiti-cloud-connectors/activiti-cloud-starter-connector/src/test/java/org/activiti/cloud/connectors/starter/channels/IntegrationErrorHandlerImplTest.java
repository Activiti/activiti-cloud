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
package org.activiti.cloud.connectors.starter.channels;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
public class IntegrationErrorHandlerImplTest {

    private static final String INTEGRATION_CONTEXT_ID = "integrationContextId";

    private IntegrationErrorHandlerImpl integrationErrorHandler;

    @Mock
    private IntegrationErrorSender integrationErrorSender;

    @Mock
    private ConnectorProperties connectorProperties;

    @BeforeEach
    public void setUp() {
        integrationErrorHandler = new IntegrationErrorHandlerImpl(integrationErrorSender,
            connectorProperties, new ObjectMapper());
    }

    @Test
    public void handleErrorMessage_should_notThrowExceptionWhenOriginalMessageIsNotIntegrationRequest() {
        //given
        ErrorMessage errorMessage = new ErrorMessage(new MessagingException(
            MessageBuilder
                .withPayload("This is not an integration request".getBytes())
                .setHeader(INTEGRATION_CONTEXT_ID, UUID.randomUUID().toString())
                .build()));

        //when
        integrationErrorHandler.accept(errorMessage);

        //then
        Mockito.verifyNoInteractions(integrationErrorSender);
    }
}
