/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannelHandler;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.service.TeamsChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("rawtypes")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AuditConsumerChannelHandlerImpl implements AuditConsumerChannelHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(AuditConsumerChannelHandlerImpl.class);

    private final EventsRepository eventsRepository;

    private final APIEventToEntityConverters eventConverters;

    private TeamsChatService teamsChatService;

    public AuditConsumerChannelHandlerImpl(
        EventsRepository eventsRepository,
        APIEventToEntityConverters eventConverters,
        TeamsChatService teamsChatService
    ) {
        this.eventsRepository = eventsRepository;
        this.eventConverters = eventConverters;
        this.teamsChatService = teamsChatService;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receiveCloudRuntimeEvent(@Headers Map<String, Object> headers, CloudRuntimeEvent<?, ?>... events) {
        if (events != null) {
            AtomicInteger counter = new AtomicInteger(0);
            List<AuditEventEntity> entities = new ArrayList<>();
            for (CloudRuntimeEvent event : events) {
                EventToEntityConverter converter = eventConverters.getConverterByEventTypeName(
                    event.getEventType().name()
                );
                if (converter != null) {
                    ((CloudRuntimeEventImpl) event).setMessageId((headers.get(MessageHeaders.ID).toString()));
                    ((CloudRuntimeEventImpl) event).setSequenceNumber(counter.getAndIncrement());
                    entities.add((AuditEventEntity) converter.convertToEntity(event));

                    if (teamsChatService != null) {
                        sendTeamsNotifcationCardToDevops((CloudRuntimeEventImpl) event);
                    }
                } else {
                    LOGGER.warn(">>> Ignoring CloudRuntimeEvents type: " + event.getEventType().name());
                }
            }
            eventsRepository.saveAll(entities);
        }
    }

    private void sendTeamsNotifcationToDevops(CloudRuntimeEventImpl event) {
        StringBuilder sb = new StringBuilder();
        sb.append("New Audit Event from " + event.getAppName() + " app * ");
        sb.append("Event Type: " + event.getEventType() + " -- ");
        CompletableFuture<Void> future = teamsChatService.sendSimpleMessage(
            "9d1a5f8a-9abc-4ed1-8dda-522ba3d2ef45",
            sb.toString()
        );
        future.join();
    }

    private void sendTeamsNotifcationCardToDevops(CloudRuntimeEventImpl event) {
        Map<String, String> processData = new LinkedHashMap<>();
        processData.put("Event Type", event.getEventType().name());
        Date date = new Date(event.getTimestamp());
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.mmm'Z'");
        processData.put("Date", f.format(date));
        processData.put("Event ID", event.getId());
        processData.put("Process Instance ID", event.getProcessInstanceId());
        CompletableFuture<Void> future = teamsChatService.sendAdaptiveCard(
            "9d1a5f8a-9abc-4ed1-8dda-522ba3d2ef45",
            //            "f924e7e2-7656-4085-a05c-612076ca2d7f",
            "HXP Audit Event",
            "A new HXP Audit event for app: " + event.getAppName(),
            processData,
            event.getActor()
        );
        future.join();
    }

    public void setTeamsChatService(TeamsChatService service) {
        this.teamsChatService = service;
    }
}
