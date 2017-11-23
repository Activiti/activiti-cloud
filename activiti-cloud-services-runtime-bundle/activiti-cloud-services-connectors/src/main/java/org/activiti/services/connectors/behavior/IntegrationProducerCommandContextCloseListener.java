package org.activiti.services.connectors.behavior;

import java.util.List;

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

    @Autowired
    public IntegrationProducerCommandContextCloseListener(ProcessEngineIntegrationChannels producer) {
        this.producer = producer;
    }

    @Override
    public void closed(CommandContext commandContext) {
        List<Message<IntegrationRequestEvent>> messages = commandContext
                .getGenericAttribute(PROCESS_ENGINE_INTEGRATION_EVENTS);

        if (messages != null) {
            for (Message<IntegrationRequestEvent> m : messages) {
                producer.integrationEventsProducer().send(m);
            }
        }
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
