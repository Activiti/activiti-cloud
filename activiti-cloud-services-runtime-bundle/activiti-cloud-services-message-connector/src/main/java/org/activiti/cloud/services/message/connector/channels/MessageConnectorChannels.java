package org.activiti.cloud.services.message.connector.channels;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface MessageConnectorChannels {

    public static final String START_MESSAGE_PAYLOAD_PRODUCER_CHANNEL = "startMessagePayloadProducerChannel";
    public static final String RECEIVE_MESSAGE_PAYLOAD_PRODUCER_CHANNEL = "receiveMessagePayloadProducerChannel";
    public static final String BPMN_MESSAGE_RECEIVED_EVENT_CONSUMER_CHANNEL = "bpmnMessageReceivedEventConsumerChannel";
    public static final String BPMN_MESSAGE_WAITING_EVENT_CONSUMER_CHANNEL = "bpmnMessageWaitingEventConsumerChannel";
    public static final String BPMN_MESSAGE_SENT_EVENT_CONSUMER_CHANNEL = "bpmnMessageSentEventConsumerChannel";
    public static final String MESSAGE_DEPLOYED_EVENT_CONSUMER_CHANNEL = "messageDeployedEventConsumerChannel";
    public static final String MESSAGE_SUBSCRIPTION_CANCELLED_EVENT_CONSUMER_CHANNEL = "messageSubscriptionCancelledEventConsumerChannel";

    interface Producer {

        @Output(START_MESSAGE_PAYLOAD_PRODUCER_CHANNEL)
        MessageChannel startMessagePayloadProducerChannel();

        @Output(RECEIVE_MESSAGE_PAYLOAD_PRODUCER_CHANNEL)
        MessageChannel receiveMessagePayloadProducerChannel();
    }
    
    interface Consumer {
        
        @Input(MESSAGE_SUBSCRIPTION_CANCELLED_EVENT_CONSUMER_CHANNEL)
        SubscribableChannel messageSubscriptionCancelledEventConsumerChannel();
        
        @Input(BPMN_MESSAGE_SENT_EVENT_CONSUMER_CHANNEL)
        SubscribableChannel bpmnMessageSentEventConsumerChannel();

        @Input(BPMN_MESSAGE_WAITING_EVENT_CONSUMER_CHANNEL)
        SubscribableChannel bpmnMessageWaitingEventConsumerChannel();

        @Input(BPMN_MESSAGE_RECEIVED_EVENT_CONSUMER_CHANNEL)
        SubscribableChannel bpmnMessageReceivedEventConsumerChannel();
        
        @Input(MESSAGE_DEPLOYED_EVENT_CONSUMER_CHANNEL)
        SubscribableChannel messageDeployedEventConsumerChannel();
        
    }
    
}
