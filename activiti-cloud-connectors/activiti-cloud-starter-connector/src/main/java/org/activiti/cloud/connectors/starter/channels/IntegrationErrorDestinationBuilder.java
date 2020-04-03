package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationRequest;

public interface IntegrationErrorDestinationBuilder {

    String buildDestination(IntegrationRequest event);

}