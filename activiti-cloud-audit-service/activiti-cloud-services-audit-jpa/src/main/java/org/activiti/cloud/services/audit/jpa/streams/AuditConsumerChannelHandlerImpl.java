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
package org.activiti.cloud.services.audit.jpa.streams;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannelHandler;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("rawtypes")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AuditConsumerChannelHandlerImpl implements AuditConsumerChannelHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditConsumerChannelHandlerImpl.class);

    private final EventsRepository eventsRepository;

    private final APIEventToEntityConverters eventConverters;

    public AuditConsumerChannelHandlerImpl(EventsRepository eventsRepository,
                                           APIEventToEntityConverters eventConverters) {
        this.eventsRepository = eventsRepository;
        this.eventConverters = eventConverters;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void accept(Message<List<CloudRuntimeEvent<?, ?>>> events) {
        if (events != null) {
            AtomicInteger counter = new AtomicInteger(0);
            List<AuditEventEntity> entities = new ArrayList<>();
            for (CloudRuntimeEvent event : events.getPayload()) {
                EventToEntityConverter converter = eventConverters.getConverterByEventTypeName(event.getEventType()
                                                                                                    .name());
                if (converter != null) {
                    ((CloudRuntimeEventImpl) event).setMessageId((events.getHeaders().get(MessageHeaders.ID)
                                                                         .toString()));
                    ((CloudRuntimeEventImpl) event).setSequenceNumber(counter.getAndIncrement());
                    entities.add((AuditEventEntity) converter.convertToEntity(event));
                } else {
                    LOGGER.warn(">>> Ignoring CloudRuntimeEvents type: " + event.getEventType()
                                                                                .name());
                }
            }
            eventsRepository.saveAll(entities);
        }
    }

}
