package org.activiti.cloud.connectors.starter;

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.connectors.starter.channels.CloudConnectorConsumerChannels;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.configuration.EnableActivitiCloudConnector;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import static org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder.resultFor;
import static org.assertj.core.api.Assertions.*;

@SpringBootApplication
@EnableActivitiCloudConnector
@EnableBinding(CloudConnectorConsumerChannels.class)
public class ActivitiCloudConnectorApp implements CommandLineRunner {

    private static final String CHANNEL_NAME = "notifications";

    private final MessageChannel runtimeCmdProducer;

    private final BinderAwareChannelResolver resolver;

    private static final String OTHER_PROCESS_DEF = "MyOtherProcessDef";

    @Autowired
    private ConnectorProperties connectorProperties;

    public ActivitiCloudConnectorApp(MessageChannel runtimeCmdProducer,
                                     BinderAwareChannelResolver resolver) {
        this.runtimeCmdProducer = runtimeCmdProducer;
        this.resolver = resolver;
    }

    public static void main(String[] args) {
        SpringApplication.run(ActivitiCloudConnectorApp.class,
                              args);
    }

    @Override
    public void run(String... args) throws Exception {
        assertThat(runtimeCmdProducer).isNotNull();
    }

    @StreamListener(value = CloudConnectorConsumerChannels.INTEGRATION_EVENT_CONSUMER, condition = "headers['type']=='Mock'")
    public void mockTypeIntegrationRequestEvents(IntegrationRequest event) {
        verifyEventAndCreateResults(event);
        Map<String, Object> resultVariables = createResultVariables(event);
        IntegrationResult integrationResultEvent = resultFor(event, connectorProperties)
                .withOutboundVariables(resultVariables)
                .build();
        Message<IntegrationResult> message = MessageBuilder.withPayload(integrationResultEvent).build();
        resolver.resolveDestination(CHANNEL_NAME).send(message);
    }

    private void verifyEventAndCreateResults(IntegrationRequest event) {
        assertThat(event.getIntegrationContext().getId()).isNotEmpty();
        assertThat(event).isNotNull();
        assertThat(event.getIntegrationContext().getProcessDefinitionId()).isNotNull();
        assertThat(event.getIntegrationContext().getProcessInstanceId()).isNotNull();
    }

    private Map<String, Object> createResultVariables(IntegrationRequest integrationRequest) {
        Map<String, Object> resultVariables = new HashMap<>();
        resultVariables.put("var1",
                            integrationRequest.getIntegrationContext().getInBoundVariables().get("var1"));
        resultVariables.put("var2",
                            Long.valueOf(integrationRequest.getIntegrationContext().getInBoundVariables().get("var2").toString()) + 1);
        return resultVariables;
    }
}
