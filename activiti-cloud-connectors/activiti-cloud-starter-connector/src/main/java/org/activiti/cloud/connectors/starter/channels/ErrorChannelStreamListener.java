package org.activiti.cloud.connectors.starter.channels;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.support.ErrorMessage;

public class ErrorChannelStreamListener {

    private static final String ERROR_CHANNEL = "errorChannel";
    
    private final IntegrationErrorHandler integrationErrorHandler;
    
    public ErrorChannelStreamListener(IntegrationErrorHandler integrationErrorSender) {
        this.integrationErrorHandler = integrationErrorSender;
    }
    
    @StreamListener(ERROR_CHANNEL)
    public void handleError(ErrorMessage errorMessage) {
        integrationErrorHandler.handleErrorMessage(errorMessage);
    }
    
}
