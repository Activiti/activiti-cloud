/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.starters.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;

public class EventsAggregator implements MessageHandler {

    private List<CloudRuntimeEvent<?, ?>> events = new ArrayList<>();

    private MyProducer producer;

    private AtomicReference<Message<?>> errorMessageRef = new AtomicReference<>();

    public EventsAggregator(MyProducer producer) {
        this.producer = producer;
    }

    public EventsAggregator addEvents(CloudRuntimeEvent<?, ?>... events) {
        this.events.addAll(Arrays.asList(events));

        return this;
    }

    public CloudRuntimeEvent<?, ?>[] sendAll() {
        List<CloudRuntimeEvent<?, ?>> sentEvents = new ArrayList<>(events);

        errorMessageRef.set(null);
        producer.send(events.toArray(new CloudRuntimeEvent<?, ?>[] {}));
        events.clear();

        return sentEvents.toArray(CloudRuntimeEvent[]::new);
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        errorMessageRef.set(message);
    }

    public Throwable getException() {
        return Optional
            .ofNullable(errorMessageRef.get())
            .map(ErrorMessage.class::cast)
            .map(ErrorMessage::getPayload)
            .orElse(null);
    }

    public EventsAggregator errorChannel(SubscribableChannel errorChannel) {
        errorChannel.subscribe(this);

        return this;
    }
}
