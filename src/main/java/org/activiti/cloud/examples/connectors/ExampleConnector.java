package org.activiti.cloud.examples.connectors;

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder;
import org.activiti.runtime.api.model.IntegrationRequest;
import org.activiti.runtime.api.model.IntegrationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import static net.logstash.logback.marker.Markers.append;

@Component
@EnableBinding(ExampleConnectorChannels.class)
public class ExampleConnector {

    private final Logger logger = LoggerFactory.getLogger(ExampleConnector.class);

    @Value("${spring.application.name}")
    private String appName;

    //track whether this instance of this connector has been called
    private boolean connectorCalled;

    @Autowired
    private ConnectorProperties connectorProperties;

    private final IntegrationResultSender integrationResultSender;

    public ExampleConnector(IntegrationResultSender integrationResultSender) {

        this.integrationResultSender = integrationResultSender;
    }

    @StreamListener(value = ExampleConnectorChannels.EXAMPLE_CONNECTOR_CONSUMER)
    public void performTask(IntegrationRequest event) throws InterruptedException {

        String text = String.valueOf(event.getIntegrationContext().getInBoundVariables().get("text"));
        logger.info(append("service-name",
                           appName),
                    ">>> In example-cloud-connector");

        connectorCalled = true;
        text += " "+ExampleConnector.class.getName()+" has been called";

        Map<String, Object> results = new HashMap<>();
        results.put("text",
                    text);
        Message<IntegrationResult> message = IntegrationResultBuilder.resultFor(event, connectorProperties)
                .withOutboundVariables(results)
                .buildMessage();
        integrationResultSender.send(message);
    }

    public boolean isConnectorCalled() {
        return connectorCalled;
    }
}
