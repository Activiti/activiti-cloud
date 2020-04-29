package org.activiti.cloud.connectors.starter.channels;

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

import com.fasterxml.jackson.databind.ObjectMapper;

public class IntegrationErrorHandlerImpl implements IntegrationErrorHandler {
    private static final String INTEGRATION_CONTEXT_ID = "integrationContextId";

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
        Optional<Message<?>> originalMessage = Optional.ofNullable(errorMessage.getOriginalMessage());

        Optional<Message<?>> failedMessage = originalMessage.isPresent() ? originalMessage
                                                                         : Optional.ofNullable(throwablePayload.getFailedMessage());
        if (failedMessage.isPresent()) {
            failedMessage.filter(this::isIntegrationRequest)
                         .map(it -> new ErrorMessage(throwablePayload, it))
                         .ifPresent(this::sendIntegrationError);
        } else {
            logger.warn("The originalMessage is empty");
        }
    }

    protected boolean isIntegrationRequest(Message<?> message) {
        return Optional.ofNullable(message)
                       .map(Message::getHeaders)
                       .map(headers -> headers.get(INTEGRATION_CONTEXT_ID))
                       .isPresent();
    }

    protected void sendIntegrationError(ErrorMessage errorMessage) {
        byte[] data = (byte[]) errorMessage.getOriginalMessage()
                                           .getPayload();
        try {
            IntegrationRequest integrationRequest = objectMapper.readValue(data,
                                                                           IntegrationRequest.class);
            Throwable cause = errorMessage.getPayload()
                                          .getCause();

            Message<IntegrationError> message = IntegrationErrorBuilder.errorFor(integrationRequest,
                                                                                 connectorProperties,
                                                                                 cause)
                                                                       .buildMessage();
            integrationErrorSender.send(message);

        } catch (Throwable cause) {
            logger.error("Error sending IntegrationError for IntegrationRequest", cause);
        }
    }

}
