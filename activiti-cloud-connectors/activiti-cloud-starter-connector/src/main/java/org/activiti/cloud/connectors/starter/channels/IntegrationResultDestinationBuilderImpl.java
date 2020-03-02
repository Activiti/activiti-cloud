package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class IntegrationResultDestinationBuilderImpl implements IntegrationResultDestinationBuilder {

    @Value("${ACT_INT_RES_CONSUMER:}")
    private String resultDestinationOverride;

    private final ConnectorProperties connectorProperties;

    @Autowired
    public IntegrationResultDestinationBuilderImpl(ConnectorProperties connectorProperties) {
        this.connectorProperties = connectorProperties;
    }
    
    @Override
    public String buildDestination(IntegrationRequest event) {
        String destination = (resultDestinationOverride == null || resultDestinationOverride.isEmpty())
                ? "integrationResult" + connectorProperties.getMqDestinationSeparator() + event.getServiceFullName() : resultDestinationOverride;
                
        return destination;
    }
    
    
}
