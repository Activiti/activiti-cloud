package org.activiti.cloud.services.messages.events.producer;

import org.activiti.api.process.model.StartMessageSubscription;
import org.activiti.api.process.model.builders.MessageEventPayloadBuilder;
import org.activiti.api.process.model.events.StartMessageDeployedEvent;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.api.process.runtime.events.listener.ProcessRuntimeEventListener;
import org.activiti.cloud.services.messages.events.MessageEventHeaders;
import org.activiti.cloud.services.messages.events.support.MessageEventsDispatcher;
import org.activiti.cloud.services.messages.events.support.StartMessageDeployedEventMessageBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;

public class StartMessageDeployedEventMessageProducer implements ProcessRuntimeEventListener<StartMessageDeployedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(BpmnMessageSentEventMessageProducer.class);

    private final StartMessageDeployedEventMessageBuilderFactory messageBuilderFactory;
    private final MessageEventsDispatcher messageEventsDispatcher;

    public StartMessageDeployedEventMessageProducer(@NonNull MessageEventsDispatcher messageEventsDispatcher,
                                                    @NonNull StartMessageDeployedEventMessageBuilderFactory messageBuilderFactory) {
        this.messageEventsDispatcher = messageEventsDispatcher;
        this.messageBuilderFactory = messageBuilderFactory;
    }
    
    @Override
    public void onEvent(StartMessageDeployedEvent event) {
        logger.debug("onEvent: {}", event);

        StartMessageSubscription messageSubscription = event.getEntity()
                                                            .getMessageSubscription();

        MessageEventPayload messageEventPayload = MessageEventPayloadBuilder.messageEvent(messageSubscription.getEventName())
                                                                            .withCorrelationKey(messageSubscription.getConfiguration())
                                                                            .build();
        
        
        Message<MessageEventPayload> message = messageBuilderFactory.create(event)
                                                                    .withPayload(messageEventPayload)
                                                                    .setHeader(MessageEventHeaders.MESSAGE_EVENT_TYPE,
                                                                               event.getEventType()
                                                                                    .name())
                                                                    .build();

        messageEventsDispatcher.dispatch(message);
    }

}
