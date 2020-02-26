package org.activiti.cloud.services.messages.core.controlbus;

import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway
public interface ControlBusGateway {

    void send(String command);

}