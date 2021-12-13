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
package org.activiti.cloud.services.audit.jpa.streams.config;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@Configuration
@ConditionalOnProperty(name = "activiti.stream.cloud.functional.binding", havingValue = "enabled")
public class AuditConsumerChannelConfiguration {

    private static Logger LOGGER = LoggerFactory.getLogger(AuditConsumerChannelConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(name = "auditConsumer")
    public Consumer<Message<CloudRuntimeEvent[]>> auditConsumer(APIEventToEntityConverters eventConverters, EventsRepository eventsRepository) {
        return (message) -> {
            if (Objects.nonNull(message.getPayload())) {
                AtomicInteger counter = new AtomicInteger(0);
                for (CloudRuntimeEvent event : message.getPayload()) {
                    EventToEntityConverter converter = eventConverters.getConverterByEventTypeName(event.getEventType().name());
                    if (converter != null) {
                        ((CloudRuntimeEventImpl<Object, Enum<?>>)event).setMessageId((message.getHeaders().get(MessageHeaders.ID).toString()));
                        ((CloudRuntimeEventImpl)event).setSequenceNumber(counter.getAndIncrement());
                        eventsRepository.save((AuditEventEntity)converter.convertToEntity(event));
                    } else {
                        LOGGER.warn(">>> Ignoring CloudRuntimeEvents type: " + event.getEventType().name());
                    }
                }
            }
        };
    }
}
