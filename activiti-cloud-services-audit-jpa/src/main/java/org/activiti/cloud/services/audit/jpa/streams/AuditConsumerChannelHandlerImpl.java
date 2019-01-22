/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.audit.jpa.streams;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskActivatedEventImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannelHandler;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannels;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@EnableBinding(AuditConsumerChannels.class)
public class AuditConsumerChannelHandlerImpl implements AuditConsumerChannelHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(AuditConsumerChannelHandlerImpl.class);

    private final EventsRepository eventsRepository;

    private final APIEventToEntityConverters eventConverters;

    @Autowired
    public AuditConsumerChannelHandlerImpl(EventsRepository eventsRepository,
                                           APIEventToEntityConverters eventConverters) {
        this.eventsRepository = eventsRepository;
        this.eventConverters = eventConverters;
    }

    @Override
    @StreamListener(AuditConsumerChannels.AUDIT_CONSUMER)
    public void receiveCloudRuntimeEvent(@Headers Map<String, Object> headers, CloudRuntimeEvent<?, ?>... events) {
        if (events != null) {
            AtomicInteger counter = new AtomicInteger(0);
            for (CloudRuntimeEvent event : events) {
                EventToEntityConverter converter = eventConverters.getConverterByEventTypeName(event.getEventType().name());
                if (converter != null) {
                    ((CloudRuntimeEventImpl)event).setMessageId((String) headers.get(MessageHeaders.ID));
                    ((CloudRuntimeEventImpl)event).setSequenceNumber(counter.getAndIncrement());
                    eventsRepository.save((AuditEventEntity) converter.convertToEntity(event));
                } else {
                    LOGGER.warn(">>> Ignoring CloudRuntimeEvents type: " + event.getEventType().name());
                }
            }
        }
    }
}