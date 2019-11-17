package org.activiti.cloud.services.message.events;

import org.activiti.api.process.model.events.StartMessageDeployedEvent;
import org.activiti.api.process.runtime.events.listener.ProcessRuntimeEventListener;
import org.activiti.cloud.api.process.model.events.CloudStartMessageDeployedEvent;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class StartMessageDeployedEventMessageProducer implements ProcessRuntimeEventListener<StartMessageDeployedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(BpmnMessageSentEventMessageProducer.class);

    private final StartMessageDeployedEventMessageBuilderFactory messageBuilderFactory;
    private final MessageChannel messageChannel;
    private final ToCloudProcessRuntimeEventConverter runtimeEventConverter;

    public StartMessageDeployedEventMessageProducer(@NonNull MessageChannel messageChannel,
                                                    @NonNull StartMessageDeployedEventMessageBuilderFactory messageBuilderFactory,
                                                    @NonNull ToCloudProcessRuntimeEventConverter runtimeEventConverter) {
        this.messageChannel = messageChannel;
        this.messageBuilderFactory = messageBuilderFactory;
        this.runtimeEventConverter = runtimeEventConverter;
    }
    
    @Override
    public void onEvent(StartMessageDeployedEvent event) {
        logger.debug("onEvent: {}", event);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("requires active transaction synchronization");
        }
        
        Message<CloudStartMessageDeployedEvent> message = messageBuilderFactory.create(event)
                                                                               .withPayload(runtimeEventConverter.from(event))
                                                                               .build();

        TransactionSynchronizationManager.registerSynchronization(new MessageSenderTransactionSynchronization(message,
                                                                                                              messageChannel));
        
    }

}
