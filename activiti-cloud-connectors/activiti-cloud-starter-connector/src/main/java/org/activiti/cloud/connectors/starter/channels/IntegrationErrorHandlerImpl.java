package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;

public class IntegrationErrorHandlerImpl implements IntegrationErrorHandler {
    private static Logger logger = LoggerFactory.getLogger(IntegrationErrorHandlerImpl.class);
    
    private final IntegrationErrorSender integrationErrorSender;
    private final ConnectorProperties connectorProperties;
    
    public IntegrationErrorHandlerImpl(IntegrationErrorSender integrationErrorSender,
                                       ConnectorProperties connectorProperties) {
        this.integrationErrorSender = integrationErrorSender;
        this.connectorProperties = connectorProperties;
    }
    
    @Override
    public void handleErrorMessage(ErrorMessage errorMessage) {
        
        Throwable errorMessagePayload = errorMessage.getPayload();
        Message<?> originalMessage = errorMessage.getOriginalMessage();

        if (originalMessage != null && IntegrationRequest.class.isInstance(originalMessage.getPayload())) {
            IntegrationRequest integrationRequest = IntegrationRequest.class.cast(originalMessage.getPayload());
            
            IntegrationError integrationError = IntegrationErrorBuilder.errorFor(integrationRequest, 
                                                                                 connectorProperties,
                                                                                 new Exception(errorMessagePayload))
                                                                       .build();
            
            Message<IntegrationError> message = MessageBuilder.withPayload(integrationError)
                                                              .build();
            integrationErrorSender.send(message);
        } else {
            logger.error("{}", errorMessagePayload);
        }
    }
    
}
