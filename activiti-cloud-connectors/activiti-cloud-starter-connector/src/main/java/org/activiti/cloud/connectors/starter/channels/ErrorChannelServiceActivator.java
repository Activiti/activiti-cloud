package org.activiti.cloud.connectors.starter.channels;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.support.ErrorMessage;

public class ErrorChannelServiceActivator {

    private static final String ERROR_CHANNEL = "errorChannel";

    private final IntegrationErrorHandler integrationErrorHandler;

    public ErrorChannelServiceActivator(IntegrationErrorHandler integrationErrorSender) {
        this.integrationErrorHandler = integrationErrorSender;
    }

    @ServiceActivator(inputChannel = ERROR_CHANNEL)
    public void handleError(ErrorMessage errorMessage) {
        integrationErrorHandler.handleErrorMessage(errorMessage);
    }
}
