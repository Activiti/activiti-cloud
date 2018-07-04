package org.activiti.cloud.starter.audit.tests.it;

import org.activiti.cloud.starters.test.StreamProducer;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(StreamProducer.class)
public class MyProducer2 {

    private final MessageChannel producer;

    @Autowired
    public MyProducer2(MessageChannel producer) {
        this.producer = producer;
    }

    public void send(CloudRuntimeEvent<?, ?>... newEvents) {
        producer.send(MessageBuilder.withPayload(newEvents).build());
    }
}