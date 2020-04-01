package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

public class IntegrationErrorHandlerImpl implements IntegrationErrorHandler {
    private static Logger logger = LoggerFactory.getLogger(IntegrationErrorHandlerImpl.class);

    private final IntegrationErrorSender integrationErrorSender;
    private final ConnectorProperties connectorProperties;
    private final ObjectMapper objectMapper;

    public IntegrationErrorHandlerImpl(IntegrationErrorSender integrationErrorSender,
                                       ConnectorProperties connectorProperties,
                                       ObjectMapper objectMapper) {
        this.integrationErrorSender = integrationErrorSender;
        this.connectorProperties = connectorProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleErrorMessage(ErrorMessage errorMessage) {
        logger.debug("Error Message exception occurred: {}", errorMessage);

        MessagingException throwablePayload = MessagingException.class.cast(errorMessage.getPayload());
        Message<?> originalMessage= errorMessage.getOriginalMessage();

        if(originalMessage == null) {
            originalMessage = throwablePayload.getFailedMessage();
        }

        if (originalMessage != null) {
            byte[] data = (byte[]) originalMessage.getPayload();
            IntegrationRequest integrationRequest = null;

            try {
                integrationRequest = objectMapper.readValue(data, IntegrationRequest.class);
            } catch (Throwable cause) {
                logger.error("Error reading IntegrationRequest", cause);

                throw new RuntimeException(cause);
            }

            Throwable cause = throwablePayload.getCause();

            Message<IntegrationError> message = IntegrationErrorBuilder.errorFor(integrationRequest,
                                                                                 connectorProperties,
                                                                                 cause)
                                                                       .buildMessage();
            integrationErrorSender.send(message);

        } else {
            logger.error("The originalMessage is empty");
        }
    }

}
