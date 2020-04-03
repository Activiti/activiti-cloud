package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.springframework.messaging.MessageChannel;

public interface IntegrationResultChannelResolver {

    MessageChannel resolveDestination(IntegrationRequest event);

}