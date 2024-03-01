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
import java.util.Optional;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

public class IntegrationErrorHandlerImpl implements IntegrationErrorHandler {

    private static final String INTEGRATION_CONTEXT_ID = "integrationContextId";

    private static Logger logger = LoggerFactory.getLogger(IntegrationErrorHandlerImpl.class);

    private final IntegrationErrorSender integrationErrorSender;
    private final ConnectorProperties connectorProperties;
    private final ObjectMapper objectMapper;

    public IntegrationErrorHandlerImpl(
        IntegrationErrorSender integrationErrorSender,
        ConnectorProperties connectorProperties,
        ObjectMapper objectMapper
    ) {
        this.integrationErrorSender = integrationErrorSender;
        this.connectorProperties = connectorProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleErrorMessage(ErrorMessage errorMessage) {
        logger.debug("Error Message exception occurred: {}", errorMessage);

        MessagingException throwablePayload = MessagingException.class.cast(errorMessage.getPayload());
        Optional<Message<?>> originalMessage = Optional.ofNullable(errorMessage.getOriginalMessage());

        Optional<Message<?>> failedMessage = originalMessage.isPresent()
            ? originalMessage
            : Optional.ofNullable(throwablePayload.getFailedMessage());
        if (failedMessage.isPresent()) {
            failedMessage
                .filter(this::isIntegrationRequest)
                .map(it -> new ErrorMessage(throwablePayload, it))
                .ifPresent(this::sendIntegrationError);
        } else {
            logger.warn("The originalMessage is empty");
        }
    }

    private boolean isIntegrationRequest(Message<?> message) {
        return Optional
            .ofNullable(message)
            .map(Message::getHeaders)
            .map(headers -> headers.get(INTEGRATION_CONTEXT_ID))
            .isPresent();
    }

    private void sendIntegrationError(ErrorMessage errorMessage) {
        byte[] data = (byte[]) errorMessage.getOriginalMessage().getPayload();
        try {
            IntegrationRequest integrationRequest = objectMapper.readValue(data, IntegrationRequest.class);
            Throwable cause = Optional
                .ofNullable(errorMessage.getPayload().getCause())
                .orElse(errorMessage.getPayload());

            Message<IntegrationError> message = IntegrationErrorBuilder
                .errorFor(integrationRequest, connectorProperties, cause)
                .buildMessage();
            integrationErrorSender.send(message);
        } catch (Throwable cause) {
            logger.error("Error sending IntegrationError for IntegrationRequest", cause);
        }
    }
}
