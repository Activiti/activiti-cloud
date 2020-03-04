package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class IntegrationErrorDestinationBuilderImpl implements IntegrationErrorDestinationBuilder {

    @Value("${ACT_INT_ERR_CONSUMER:}")
    private String resultDestinationOverride;

    private final ConnectorProperties connectorProperties;

    @Autowired
    public IntegrationErrorDestinationBuilderImpl(ConnectorProperties connectorProperties) {
        this.connectorProperties = connectorProperties;
    }

    @Override
    public String buildDestination(IntegrationRequest event) {
        String destination = (resultDestinationOverride == null || resultDestinationOverride.isEmpty())
                ? "integrationError" + connectorProperties.getMqDestinationSeparator() + event.getServiceFullName() : resultDestinationOverride;

        return destination;
    }

    public String getResultDestinationOverride() {
        return resultDestinationOverride;
    }

    public void setResultDestinationOverride(String resultDestinationOverride) {
        this.resultDestinationOverride = resultDestinationOverride;
    }
}
