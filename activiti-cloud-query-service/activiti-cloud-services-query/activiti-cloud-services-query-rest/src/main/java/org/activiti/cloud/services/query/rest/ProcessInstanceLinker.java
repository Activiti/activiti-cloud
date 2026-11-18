/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.rest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.services.query.app.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.QueryEntityNotFoundException;
import org.activiti.cloud.services.query.app.specification.ProcessInstanceSpecification;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.support.TransactionTemplate;

public class ProcessInstanceLinker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceLinker.class);

    private final ProcessInstanceRepository processInstanceRepository;

    private final ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

    private final TransactionTemplate transactionTemplate;

    private final ThreadPoolTaskScheduler retryScheduler;

    private final int maxRetries;

    private final int retryDelaySeconds;

    public ProcessInstanceLinker(
        ProcessInstanceRepository processInstanceRepository,
        ProcessInstanceHierarchyRepository processInstanceHierarchyRepository,
        TransactionTemplate transactionTemplate,
        int maxRetries,
        int retryDelaySeconds,
        ThreadPoolTaskScheduler retryScheduler
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceHierarchyRepository = processInstanceHierarchyRepository;
        this.transactionTemplate = transactionTemplate;
        this.retryScheduler = retryScheduler;
        this.maxRetries = maxRetries;
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public void shutdown() {
        retryScheduler.shutdown();
    }

    public void linkProcessInstances(
        String mainProcessInstanceId,
        List<String> processInstanceIds,
        String linkProcessInstanceType,
        String userId
    ) {
        linkProcessInstances(mainProcessInstanceId, processInstanceIds, linkProcessInstanceType, userId, 0);
    }

    private void linkProcessInstances(
        String mainProcessInstanceId,
        List<String> processInstanceIds,
        String linkProcessInstanceType,
        String userId,
        int attempt
    ) {
        try {
            transactionTemplate.executeWithoutResult(_ ->
                link(mainProcessInstanceId, processInstanceIds, linkProcessInstanceType, userId)
            );
        } catch (RuntimeException e) {
            checkAttemptsAndScheduleRetry(
                mainProcessInstanceId,
                processInstanceIds,
                linkProcessInstanceType,
                userId,
                attempt,
                e
            );
        }
    }

    private void checkAttemptsAndScheduleRetry(
        String mainProcessInstanceId,
        List<String> processInstanceIds,
        String linkProcessInstanceType,
        String userId,
        int attempt,
        RuntimeException e
    ) {
        if (attempt < maxRetries) {
            LOGGER.warn(
                "Unable to link process instances to main process instance id '{}', retrying in {}s",
                mainProcessInstanceId,
                retryDelaySeconds,
                e
            );
            retryScheduler.schedule(
                () ->
                    linkProcessInstances(
                        mainProcessInstanceId,
                        processInstanceIds,
                        linkProcessInstanceType,
                        userId,
                        attempt + 1
                    ),
                Instant.now().plusSeconds(retryDelaySeconds)
            );
        } else {
            LOGGER.error(
                "Unable to link process instances to main process instance id '{}', giving up",
                mainProcessInstanceId,
                e
            );
        }
    }

    private void link(
        String mainProcessInstanceId,
        List<String> processInstanceIds,
        String linkProcessInstanceType,
        String userId
    ) {
        if (processInstanceIds == null || processInstanceIds.isEmpty()) {
            LOGGER.debug(
                "No process instance id provided to link to main process instance id '{}'",
                mainProcessInstanceId
            );
            return;
        }

        var mainProcess = processInstanceRepository
            .findById(mainProcessInstanceId)
            .orElseThrow(() ->
                new QueryEntityNotFoundException(
                    "Unable to find process for the given id:'" + mainProcessInstanceId + "'"
                )
            );

        var request = new ProcessInstanceSearchRequest();
        request.setId(new HashSet<>(processInstanceIds));
        request.setLinkedProcessInstanceType(Set.of(linkProcessInstanceType));

        var orphanProcesses = processInstanceRepository.findAll(
            ProcessInstanceSpecification.restricted(request, userId)
        );

        if (orphanProcesses.isEmpty()) {
            LOGGER.debug("No process instance found for the given ids:'{}'", processInstanceIds);
            return;
        }

        List<ProcessInstanceEntity> toLink = new ArrayList<>();
        orphanProcesses.forEach(orphanProcess -> {
            if (orphanProcess.getLinkedProcessInstanceId() != null) {
                LOGGER.debug(
                    "Process instance id {} is already linked to {}",
                    orphanProcess.getId(),
                    orphanProcess.getLinkedProcessInstanceId()
                );
            } else if (Objects.equals(orphanProcess.getInitiator(), mainProcess.getInitiator())) {
                orphanProcess.setLinkedProcessInstanceId(mainProcess.getId());
                toLink.add(orphanProcess);
            } else {
                LOGGER.debug(
                    "User {} not permitted to link process instance id {} to main process instance id {}",
                    userId,
                    orphanProcess.getId(),
                    mainProcessInstanceId
                );
            }
        });
        processInstanceRepository.saveAll(toLink);
        registerInHierarchy(mainProcess.getId(), toLink.stream().map(ProcessInstanceEntity::getId).toList());
    }

    private void registerInHierarchy(String mainProcessInstanceId, List<String> linkedIds) {
        if (linkedIds.isEmpty()) {
            return;
        }

        var ancestors = processInstanceHierarchyRepository.findByDescendantId(mainProcessInstanceId);
        var descendants = processInstanceHierarchyRepository.findByAncestorIdInAndDepthGreaterThan(linkedIds, -1);

        Map<ProcessInstanceHierarchyId, ProcessInstanceHierarchyEntity> candidates = new LinkedHashMap<>();
        for (var ancestor : ancestors) {
            for (var descendant : descendants) {
                var candidate = new ProcessInstanceHierarchyEntity(
                    ancestor.getAncestorId(),
                    descendant.getDescendantId(),
                    ancestor.getDepth() + 1 + descendant.getDepth(),
                    descendant.getDepth() == 0
                        ? ProcessInstanceHierarchyEntity.RELATION_LINKED
                        : descendant.getRelationType()
                );
                candidates.merge(idOf(candidate), candidate, (a, b) -> a.getDepth() <= b.getDepth() ? a : b);
            }
        }

        Set<ProcessInstanceHierarchyId> existing = processInstanceHierarchyRepository
            .findAllById(candidates.keySet())
            .stream()
            .map(ProcessInstanceLinker::idOf)
            .collect(Collectors.toSet());

        processInstanceHierarchyRepository.saveAll(
            candidates
                .values()
                .stream()
                .filter(candidate -> !existing.contains(idOf(candidate)))
                .toList()
        );
    }

    private static ProcessInstanceHierarchyId idOf(ProcessInstanceHierarchyEntity entity) {
        return new ProcessInstanceHierarchyId(entity.getAncestorId(), entity.getDescendantId());
    }
}
