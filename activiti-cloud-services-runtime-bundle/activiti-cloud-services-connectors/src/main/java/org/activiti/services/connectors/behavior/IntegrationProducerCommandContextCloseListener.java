package org.activiti.services.connectors.behavior;

import java.util.List;

import org.activiti.cloud.services.events.IntegrationRequestSentEventImpl;
import org.activiti.cloud.services.events.configuration.ApplicationProperties;
import org.activiti.cloud.services.events.listeners.CommandContextEventsAggregator;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.activiti.services.connectors.channel.ProcessEngineIntegrationChannels;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class IntegrationProducerCommandContextCloseListener implements CommandContextCloseListener {

    public static final String PROCESS_ENGINE_INTEGRATION_EVENTS = "processEngineIntegrationEvents";

    private final ProcessEngineIntegrationChannels producer;
    private final CommandContextEventsAggregator eventsAggregator;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public IntegrationProducerCommandContextCloseListener(ProcessEngineIntegrationChannels producer,
                                                          CommandContextEventsAggregator eventsAggregator,
                                                          ApplicationProperties applicationProperties) {
        this.producer = producer;
        this.eventsAggregator = eventsAggregator;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void closed(CommandContext commandContext) {
        List<Message<IntegrationRequestEvent>> messages = commandContext
                .getGenericAttribute(PROCESS_ENGINE_INTEGRATION_EVENTS);

        if (messages != null) {
            for (Message<IntegrationRequestEvent> message : messages) {
                producer.integrationEventsProducer().send(message);
                registerIntegrationRequestSentEvent(message);
            }
        }
    }

    private void registerIntegrationRequestSentEvent(Message<IntegrationRequestEvent> message) {
        IntegrationRequestEvent integrationRequestEvent = message.getPayload();
        IntegrationRequestSentEventImpl event = new IntegrationRequestSentEventImpl(applicationProperties.getName(),
                                                                                    integrationRequestEvent.getExecutionId(),
                                                                                    integrationRequestEvent.getProcessDefinitionId(),
                                                                                    integrationRequestEvent.getProcessInstanceId(),
                                                                                    integrationRequestEvent.getIntegrationContextId());
        eventsAggregator.add(event);
    }

    @Override
    public void closing(CommandContext commandContext) {
        // No need to implement this method in this class
    }

    @Override
    public void afterSessionsFlush(CommandContext commandContext) {
        // No need to implement this method in this class
    }

    @Override
    public void closeFailure(CommandContext commandContext) {
        // No need to implement this method in this class
    }
}
