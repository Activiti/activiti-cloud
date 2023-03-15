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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannelHandler;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannels;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.streams.AuditConsumerChannelHandlerImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

@Configuration
public class AuditJPAStreamsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditConsumerChannelHandler auditConsumerChannelHandler(
        EventsRepository eventsRepository,
        APIEventToEntityConverters eventConverters
    ) {
        return new AuditConsumerChannelHandlerImpl(eventsRepository, eventConverters);
    }

    @FunctionBinding(input = AuditConsumerChannels.AUDIT_CONSUMER)
    @Bean
    public Consumer<Message<List<CloudRuntimeEvent<?, ?>>>> auditConsumerChannelHandlerConsumer(
        AuditConsumerChannelHandler handler
    ) {
        return message -> {
            handler.receiveCloudRuntimeEvent(
                message.getHeaders(),
                Optional
                    .ofNullable(message.getPayload())
                    .orElse(Collections.emptyList())
                    .toArray(new CloudRuntimeEvent[0])
            );
        };
    }
}
