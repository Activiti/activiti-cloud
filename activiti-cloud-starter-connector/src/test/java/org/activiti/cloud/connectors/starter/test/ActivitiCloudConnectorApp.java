package org.activiti.cloud.connectors.starter.test;

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.connectors.starter.channels.CloudConnectorConsumerChannels;
import org.activiti.cloud.connectors.starter.configuration.EnableActivitiCloudConnector;
import org.activiti.cloud.connectors.starter.model.IntegrationRequestEvent;
import org.activiti.cloud.connectors.starter.model.IntegrationResultEvent;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import static org.activiti.cloud.connectors.starter.model.IntegrationResultEventBuilder.resultFor;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootApplication
@EnableActivitiCloudConnector
@EnableBinding(CloudConnectorConsumerChannels.class)
@ComponentScan("org.activiti.cloud.connectors.starter")
public class ActivitiCloudConnectorApp implements CommandLineRunner {

    private static final String CHANNEL_NAME = "notifications";

    private final MessageChannel runtimeCmdProducer;

    private final BinderAwareChannelResolver resolver;

    private static final String OTHER_PROCESS_DEF = "MyOtherProcessDef";

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
    public void mockTypeIntegrationRequestEvents(IntegrationRequestEvent event) {
        verifyEventAndCreateResults(event);
        Map<String, Object> resultVariables = createResultVariables(event);
        IntegrationResultEvent integrationResultEvent = resultFor(event)
                .withVariables(resultVariables)
                .build();
        Message<IntegrationResultEvent> message = MessageBuilder.withPayload(integrationResultEvent).build();
        resolver.resolveDestination(CHANNEL_NAME).send(message);
    }

    /*
     * A Cloud Connector receiving Integration Events is free to Start Process Instances and interact with different Runtime Bundles
     */
    @StreamListener(value = CloudConnectorConsumerChannels.INTEGRATION_EVENT_CONSUMER, condition = "headers['type']=='MockProcessRuntime'")
    public void mockTypeIntegrationRequestEventsStartProcess(IntegrationRequestEvent event) {
        verifyEventAndCreateResults(event);
        Map<String, Object> resultVariables = createResultVariables(event);

        StartProcessInstanceCmd startProcessInstanceCmd = new StartProcessInstanceCmd(OTHER_PROCESS_DEF,
                                                                                      resultVariables);

        runtimeCmdProducer.send(MessageBuilder.withPayload(startProcessInstanceCmd).build());

        IntegrationResultEvent integrationResultEvent = resultFor(event)
                .withVariables(resultVariables)
                .build();
        Message<IntegrationResultEvent> message = MessageBuilder.withPayload(integrationResultEvent).build();
        resolver.resolveDestination(CHANNEL_NAME).send(message);
    }

    private void verifyEventAndCreateResults(IntegrationRequestEvent event) {
        assertThat(event.getId()).isNotEmpty();
        assertThat(event).isNotNull();
        assertThat(event.getExecutionId()).isNotNull();
        assertThat(event.getProcessDefinitionId()).isNotNull();
        assertThat(event.getProcessInstanceId()).isNotNull();
    }

    private Map<String, Object> createResultVariables(IntegrationRequestEvent event) {
        Map<String, Object> resultVariables = new HashMap<>();
        resultVariables.put("var1",
                            event.getVariables().get("var1"));
        resultVariables.put("var2",
                            Long.valueOf(event.getVariables().get("var2").toString()) + 1);
        return resultVariables;
    }
}
