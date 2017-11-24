package org.activiti.services.connectors.behavior;

import java.util.List;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.ApplicationProperties;
import org.activiti.cloud.services.events.integration.IntegrationRequestSentEventImpl;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.activiti.services.connectors.channel.ProcessEngineIntegrationChannels;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class IntegrationProducerCommandContextCloseListener implements CommandContextCloseListener {

    public static final String PROCESS_ENGINE_INTEGRATION_EVENTS = "processEngineIntegrationEvents";

    private final ProcessEngineIntegrationChannels integrationChannels;
    private final ProcessEngineChannels processEngineChannels;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public IntegrationProducerCommandContextCloseListener(ProcessEngineIntegrationChannels integrationChannels,
                                                          ProcessEngineChannels processEngineChannels,
                                                          ApplicationProperties applicationProperties) {
        this.integrationChannels = integrationChannels;
        this.processEngineChannels = processEngineChannels;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void closed(CommandContext commandContext) {
        List<Message<IntegrationRequestEvent>> messages = commandContext
                .getGenericAttribute(PROCESS_ENGINE_INTEGRATION_EVENTS);

        if (messages != null) {
            for (Message<IntegrationRequestEvent> message : messages) {
                integrationChannels.integrationEventsProducer().send(message);
                sendIntegrationRequestSentEvent(message);
            }
        }
    }

    private void sendIntegrationRequestSentEvent(Message<IntegrationRequestEvent> message) {
        if (applicationProperties.isIntegrationAuditEventsEnabled()) {
            IntegrationRequestEvent integrationRequestEvent = message.getPayload();
            IntegrationRequestSentEventImpl event = new IntegrationRequestSentEventImpl(applicationProperties.getName(),
                                                                                        integrationRequestEvent.getExecutionId(),
                                                                                        integrationRequestEvent.getProcessDefinitionId(),
                                                                                        integrationRequestEvent.getProcessInstanceId(),
                                                                                        integrationRequestEvent.getIntegrationContextId());
            processEngineChannels.auditProducer().send(
                    MessageBuilder.withPayload(new ProcessEngineEvent[]{event}).build()
            );
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
