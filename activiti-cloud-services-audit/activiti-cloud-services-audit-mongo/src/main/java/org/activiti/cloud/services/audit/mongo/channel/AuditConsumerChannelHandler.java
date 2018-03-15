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

package org.activiti.cloud.services.audit.mongo.channel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.cloud.services.audit.mongo.events.ProcessEngineEventDocument;
import org.activiti.cloud.services.audit.mongo.repository.EventsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(AuditConsumerChannels.class)
public class AuditConsumerChannelHandler {

    private final EventsRepository eventsRepository;

    @Autowired
    public AuditConsumerChannelHandler(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    @StreamListener(AuditConsumerChannels.AUDIT_CONSUMER)
    public synchronized void receive(ProcessEngineEventDocument[] events) {
        List<ProcessEngineEventDocument> incomingEvents = Arrays.asList(events);
        List<ProcessEngineEventDocument> nonIgnoredEvents = incomingEvents.stream().filter(event -> !event.isIgnored()).collect(Collectors.toList());
        eventsRepository.saveAll(nonIgnoredEvents);
    }
}