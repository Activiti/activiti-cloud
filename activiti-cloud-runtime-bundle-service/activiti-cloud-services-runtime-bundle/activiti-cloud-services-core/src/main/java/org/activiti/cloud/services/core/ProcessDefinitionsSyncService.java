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

package org.activiti.cloud.services.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.events.ProcessDeployedEvent;
import org.activiti.api.runtime.event.impl.ProcessDeployedEventImpl;
import org.activiti.api.runtime.event.impl.ProcessDeployedEvents;
import org.activiti.engine.RepositoryService;
import org.activiti.runtime.api.model.impl.APIProcessDefinitionConverter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StreamUtils;

public class ProcessDefinitionsSyncService {

    private final RepositoryService repositoryService;
    private final APIProcessDefinitionConverter converter;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ProcessDefinitionsSyncService(
        RepositoryService repositoryService,
        APIProcessDefinitionConverter converter,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.repositoryService = repositoryService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.converter = converter;
    }

    @Async
    public void syncProcessDefinitions(List<String> excludedProcessDefinitionIds) {
        toProcessDeployedEventList(excludedProcessDefinitionIds)
            .map(ProcessDeployedEvents::new)
            .forEach(applicationEventPublisher::publishEvent);
    }

    private Stream<List<ProcessDeployedEvent>> toProcessDeployedEventList(List<String> excludedProcessDefinitionIds) {
        final AtomicInteger counter = new AtomicInteger();

        return repositoryService
            .createProcessDefinitionQuery()
            .list()
            .stream()
            .filter(it -> !excludedProcessDefinitionIds.contains(it.getId()))
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 10))
            .values()
            .stream()
            .map(converter::from)
            .map(processDefinitions -> processDefinitions.stream().map(this::toProcessDeployedEvent).toList());
    }

    private ProcessDeployedEvent toProcessDeployedEvent(ProcessDefinition processDefinition) {
        try (InputStream inputStream = repositoryService.getProcessModel(processDefinition.getId())) {
            String xmlModel = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            return new ProcessDeployedEventImpl(processDefinition, xmlModel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
