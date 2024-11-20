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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.events.ProcessDeployedEvent;
import org.activiti.api.runtime.event.impl.ProcessDeployedEventImpl;
import org.activiti.api.runtime.event.impl.ProcessDeployedEvents;
import org.activiti.cloud.api.process.model.impl.SyncCloudProcessDefinitionsPayload;
import org.activiti.engine.RepositoryService;
import org.activiti.runtime.api.model.impl.APIProcessDefinitionConverter;
import org.springframework.context.ApplicationEventPublisher;
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

    public List<String> syncProcessDefinitions(SyncCloudProcessDefinitionsPayload payload) {
        List<String> excludedProcessDefinitionIds = payload.getExcludedProcessDefinitionIds();
        var processDefinitionKeys = payload.getProcessDefinitionKeys();

        return queryProcessDefinitionsStream(processDefinitionKeys, excludedProcessDefinitionIds)
            .map(processDefinitions -> processDefinitions.stream().map(this::toProcessDeployedEvent).toList())
            .map(ProcessDeployedEvents::new)
            .peek(applicationEventPublisher::publishEvent)
            .flatMap(it -> it.getProcessDeployedEvents().stream())
            .map(ProcessDeployedEvent::getProcessDefinitionId)
            .toList();
    }

    private Stream<List<ProcessDefinition>> queryProcessDefinitionsStream(
        List<String> processDefinitionKeys,
        List<String> excludedProcessDefinitionIds
    ) {
        final AtomicInteger counter = new AtomicInteger();

        var query = repositoryService.createProcessDefinitionQuery();

        Optional
            .ofNullable(processDefinitionKeys)
            .filter(Predicate.not(Collection::isEmpty))
            .map(Set::copyOf)
            .ifPresent(query::processDefinitionKeys);

        return query
            .list()
            .stream()
            .filter(it ->
                Optional.ofNullable(excludedProcessDefinitionIds).map(list -> !list.contains(it.getId())).orElse(true)
            )
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 10))
            .values()
            .stream()
            .map(converter::from);
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
