package org.activiti.cloud.services.messages.core.support;

import java.util.function.Predicate;

import org.activiti.api.process.model.events.BPMNMessageEvent.MessageEvents;
import org.activiti.api.process.model.events.MessageDefinitionEvent.MessageDefinitionEvents;
import org.activiti.api.process.model.events.MessageSubscriptionEvent.MessageSubscriptionEvents;
import org.activiti.cloud.services.messages.core.support.MessageSelectors.MessageEventTypeSelector;
import org.springframework.messaging.Message;

public class Predicates {

    public static Predicate<Message<?>> MESSAGE_WAITING = predicate(new MessageEventTypeSelector(MessageEvents.MESSAGE_WAITING)::accept); 
    public static Predicate<Message<?>> MESSAGE_RECEIVED = predicate(new MessageEventTypeSelector(MessageEvents.MESSAGE_RECEIVED)::accept); 
    public static Predicate<Message<?>> MESSAGE_SENT = predicate(new MessageEventTypeSelector(MessageEvents.MESSAGE_SENT)::accept); 
    public static Predicate<Message<?>> MESSAGE_SUBSCRIPTION_CANCELLED = predicate(new MessageEventTypeSelector(MessageSubscriptionEvents.MESSAGE_SUBSCRIPTION_CANCELLED)::accept); 
    public static Predicate<Message<?>> START_MESSAGE_DEPLOYED = predicate(new MessageEventTypeSelector(MessageDefinitionEvents.START_MESSAGE_DEPLOYED)::accept); 
    
    public static <T> Predicate<T> predicate(Predicate<T> predicate) {
        return predicate;
    }    
    public static <R> Predicate<R> not(Predicate<R> predicate) {
        return predicate.negate();
    }
    
}
