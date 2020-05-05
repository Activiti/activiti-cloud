package org.activiti.cloud.services.core.utils;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

public class MockSubscribableChannel implements SubscribableChannel {
    @Override
    public boolean subscribe(MessageHandler messageHandler) {
        return false;
    }

    @Override
    public boolean unsubscribe(MessageHandler messageHandler) {
        return false;
    }

    @Override
    public boolean send(Message<?> message, long l) {
        return false;
    }
}
