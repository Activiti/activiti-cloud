package org.activiti.cloud.connectors.starter.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.activiti.cloud.connectors.starter.channels.CloudConnectorChannels;
import org.activiti.cloud.connectors.starter.configuration.EnableActivitiCloudConnector;
import org.activiti.cloud.connectors.starter.model.IntegrationRequestEvent;
import org.activiti.cloud.connectors.starter.model.IntegrationResultEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootApplication
@EnableActivitiCloudConnector
@ComponentScan("org.activiti.cloud.connectors.starter")
public class ActivitiCloudConnectorApp implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ActivitiCloudConnectorApp.class,
                              args);
    }

    @Override
    public void run(String... args) throws Exception {

    }

    @Autowired
    private MessageChannel integrationResultsProducer;

    @StreamListener(value = CloudConnectorChannels.INTEGRATION_EVENT_CONSUMER, condition = "headers['type']=='Mock'")
    public void mockTypeIntegrationRequestEventsWithProcessDefIdHeader(IntegrationRequestEvent event,
                                                                       @Header("processDefinitionId") String processDefinitionId) {
        System.out.println("Integration Request Event Recieved: ");
        System.out.println("\t Event:  " + event.toString());
        System.out.println("\t Type:  " + "Mock");
        System.out.println("\t ProcessDefId in Header:  " + processDefinitionId);
        Map<String, Object> resultVariables = new HashMap<String, Object>();
        resultVariables.put("var1",
                            event.getVariables().get("var1"));
        resultVariables.put("var2",
                            Long.valueOf(event.getVariables().get("var2").toString()) + 1);
        IntegrationResultEvent integrationResultEvent = new IntegrationResultEvent(UUID.randomUUID().toString(),
                                                                                   event.getExecutionId(),
                                                                                   resultVariables);
        Message<IntegrationResultEvent> message = MessageBuilder.withPayload(integrationResultEvent).build();
        integrationResultsProducer.send(message);
    }

    @StreamListener(value = CloudConnectorChannels.INTEGRATION_EVENT_CONSUMER)
    public void consumePaymentIntegrationEvents(IntegrationRequestEvent event) {
        System.out.println("Integration Request Event Recieved: (no filter) ");
        System.out.println("\t Event:  " + event.toString());
    }
}
