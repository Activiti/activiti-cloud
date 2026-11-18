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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.services.query.app.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.specification.ProcessInstanceSpecification;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessInstanceLinker {

    private static final String UNABLE_TO_FIND_PROCESS_FOR_THE_GIVEN_ID = "Unable to find process for the given id:'";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceLinker.class);

    private final ProcessInstanceRepository processInstanceRepository;

    private final ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

    private final EntityFinder entityFinder;

    public ProcessInstanceLinker(
        ProcessInstanceRepository processInstanceRepository,
        ProcessInstanceHierarchyRepository processInstanceHierarchyRepository,
        EntityFinder entityFinder
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceHierarchyRepository = processInstanceHierarchyRepository;
        this.entityFinder = entityFinder;
    }

    public void linkProcessInstances(
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

        var mainProcess = entityFinder.findById(
            processInstanceRepository,
            mainProcessInstanceId,
            UNABLE_TO_FIND_PROCESS_FOR_THE_GIVEN_ID + mainProcessInstanceId + "'"
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

        List<String> linkedIds = new ArrayList<>();
        orphanProcesses.forEach(orphanProcess -> {
            if (orphanProcess.getInitiator().equals(mainProcess.getInitiator())) {
                orphanProcess.setLinkedProcessInstanceId(mainProcess.getId());
                processInstanceRepository.save(orphanProcess);
                linkedIds.add(orphanProcess.getId());
            } else {
                LOGGER.debug(
                    "User {} not permitted to link process instance id {} to main process instance id {}",
                    userId,
                    orphanProcess.getId(),
                    mainProcessInstanceId
                );
            }
        });
        registerInHierarchy(mainProcess.getId(), linkedIds);
    }

    private void registerInHierarchy(String mainProcessInstanceId, List<String> linkedIds) {
        if (linkedIds.isEmpty()) {
            return;
        }

        var ancestors = processInstanceHierarchyRepository.findByDescendantId(mainProcessInstanceId);
        var descendants = processInstanceHierarchyRepository.findByAncestorIdInAndDepthGreaterThan(linkedIds, -1);

        var candidates = new ArrayList<ProcessInstanceHierarchyEntity>();
        for (var ancestor : ancestors) {
            for (var descendant : descendants) {
                candidates.add(
                    new ProcessInstanceHierarchyEntity(
                        ancestor.getAncestorId(),
                        descendant.getDescendantId(),
                        ancestor.getDepth() + 1 + descendant.getDepth(),
                        descendant.getDepth() == 0
                            ? ProcessInstanceHierarchyEntity.RELATION_LINKED
                            : descendant.getRelationType()
                    )
                );
            }
        }

        Set<ProcessInstanceHierarchyId> existing = processInstanceHierarchyRepository
            .findAllById(candidates.stream().map(ProcessInstanceLinker::idOf).toList())
            .stream()
            .map(ProcessInstanceLinker::idOf)
            .collect(Collectors.toSet());

        processInstanceHierarchyRepository.saveAll(
            candidates
                .stream()
                .filter(candidate -> !existing.contains(idOf(candidate)))
                .toList()
        );
    }

    private static ProcessInstanceHierarchyId idOf(ProcessInstanceHierarchyEntity entity) {
        return new ProcessInstanceHierarchyId(entity.getAncestorId(), entity.getDescendantId());
    }
}
