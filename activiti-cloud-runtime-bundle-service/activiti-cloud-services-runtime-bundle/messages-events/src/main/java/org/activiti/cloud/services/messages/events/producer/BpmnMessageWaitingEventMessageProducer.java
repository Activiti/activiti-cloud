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
package org.activiti.cloud.services.messages.events.producer;

import org.activiti.api.process.model.events.BPMNMessageWaitingEvent;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.api.process.runtime.events.listener.BPMNElementEventListener;
import org.activiti.cloud.services.messages.events.MessageEventHeaders;
import org.activiti.cloud.services.messages.events.support.BpmnMessageEventMessageBuilderFactory;
import org.activiti.cloud.services.messages.events.support.MessageEventsDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;

public class BpmnMessageWaitingEventMessageProducer implements BPMNElementEventListener<BPMNMessageWaitingEvent> {

    private static final Logger logger = LoggerFactory.getLogger(BpmnMessageWaitingEventMessageProducer.class);

    private final BpmnMessageEventMessageBuilderFactory messageBuilderFactory;
    private final MessageEventsDispatcher messageEventsDispatcher;

    public BpmnMessageWaitingEventMessageProducer(
        @NonNull MessageEventsDispatcher messageEventsDispatcher,
        @NonNull BpmnMessageEventMessageBuilderFactory messageBuilderFactory
    ) {
        this.messageEventsDispatcher = messageEventsDispatcher;
        this.messageBuilderFactory = messageBuilderFactory;
    }

    @Override
    public void onEvent(BPMNMessageWaitingEvent event) {
        logger.debug("onEvent: {}", event);

        Message<MessageEventPayload> message = messageBuilderFactory
            .create(event.getEntity())
            .withPayload(event.getEntity().getMessagePayload())
            .setHeader(MessageEventHeaders.MESSAGE_EVENT_TYPE, event.getEventType().name())
            .build();

        messageEventsDispatcher.dispatch(message);
    }
}
