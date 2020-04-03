package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationRequest;

public interface IntegrationResultDestinationBuilder {

    String buildDestination(IntegrationRequest event);

}