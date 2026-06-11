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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository.RelatedProcessCountProjection;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.rest.specification.ProcessInstanceSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class ProcessInstanceSearchService {

    private final ProcessInstanceRepository processInstanceRepository;

    private final ProcessVariableService processVariableService;

    private final SecurityManager securityManager;

    private final ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

    public ProcessInstanceSearchService(
        ProcessInstanceRepository processInstanceRepository,
        ProcessVariableService processVariableService,
        SecurityManager securityManager,
        ProcessInstanceHierarchyRepository processInstanceHierarchyRepository
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.processVariableService = processVariableService;
        this.securityManager = securityManager;
        this.processInstanceHierarchyRepository = processInstanceHierarchyRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> searchRestricted(ProcessInstanceSearchRequest searchRequest, Pageable pageable) {
        return search(
            searchRequest.getProcessVariableKeys(),
            pageable,
            ProcessInstanceSpecification.restricted(searchRequest, securityManager.getAuthenticatedUserId())
        );
    }

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> searchUnrestricted(
        ProcessInstanceSearchRequest searchRequest,
        Pageable pageable
    ) {
        return search(
            searchRequest.getProcessVariableKeys(),
            pageable,
            ProcessInstanceSpecification.unrestricted(searchRequest)
        );
    }

    private Page<ProcessInstanceEntity> search(
        Set<ProcessVariableKey> processVariableKeys,
        Pageable pageable,
        ProcessInstanceSpecification specification
    ) {
        Page<ProcessInstanceEntity> processInstances = processInstanceRepository.findAll(specification, pageable);
        processVariableService.fetchProcessVariablesForProcessInstances(
            processInstances.getContent(),
            processVariableKeys
        );
        return processInstances;
    }

    @Transactional(readOnly = true)
    public Long countRestricted(ProcessInstanceSearchRequest searchRequest) {
        ProcessInstanceSpecification restrictedSpecification = ProcessInstanceSpecification.restricted(
            searchRequest,
            securityManager.getAuthenticatedUserId()
        );
        return processInstanceRepository.count(restrictedSpecification);
    }

    @Transactional(readOnly = true)
    public Long countUnrestricted(ProcessInstanceSearchRequest searchRequest) {
        ProcessInstanceSpecification unrestrictedSpecification = ProcessInstanceSpecification.unrestricted(
            searchRequest
        );
        return processInstanceRepository.count(unrestrictedSpecification);
    }

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> unrestrictedLinkedProcesses(
        Set<String> linkedProcessInstanceIds,
        Pageable pageable
    ) {
        if (linkedProcessInstanceIds == null || linkedProcessInstanceIds.isEmpty()) {
            return Page.empty(pageable);
        }

        ProcessInstanceSpecification unrestrictedSpecification = ProcessInstanceSpecification.unrestrictedLinkedProcesses(
            linkedProcessInstanceIds
        );

        return processInstanceRepository.findAll(unrestrictedSpecification, pageable);
    }

    @Transactional(readOnly = true)
    public List<ProcessInstanceEntity> unrestrictedLinkedProcesses(Set<String> linkedProcessInstanceIds) {
        if (linkedProcessInstanceIds == null || linkedProcessInstanceIds.isEmpty()) {
            return List.of();
        }

        ProcessInstanceSpecification unrestrictedSpecification = ProcessInstanceSpecification.unrestrictedLinkedProcesses(
            linkedProcessInstanceIds
        );

        return processInstanceRepository.findAll(unrestrictedSpecification);
    }

    /**
     * Populates {@code subprocesses} and {@code linkedProcesses} on every entity in the page
     * restricting the descendant batch-fetch to processes visible to the currently authenticated
     * user (user-facing endpoint use).
     */
    @Transactional(readOnly = true)
    public void enrichWithRelatedProcessesRestricted(Page<ProcessInstanceEntity> processInstances) {
        doEnrich(processInstances, securityManager.getAuthenticatedUserId());
    }

    /**
     * Counts, for every ancestor in {@code ancestorIds}, how many descendants exist at any depth,
     * grouped by relation type ({@code subprocess} / {@code linked}). One closure-table query, no
     * descendant entities fetched — used by the admin search endpoint to expose counts only.
     *
     * @return ancestorId → (relationType → count); ancestors with no descendants are absent.
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> countRelatedProcessesByAncestor(Set<String> ancestorIds) {
        if (ancestorIds == null || ancestorIds.isEmpty()) {
            return Map.of();
        }
        return processInstanceHierarchyRepository
            .countRelatedByAncestor(ancestorIds)
            .stream()
            .collect(
                Collectors.groupingBy(
                    RelatedProcessCountProjection::getAncestorId,
                    Collectors.toMap(
                        RelatedProcessCountProjection::getRelationType,
                        RelatedProcessCountProjection::getRelatedCount
                    )
                )
            );
    }

    private void doEnrich(Page<ProcessInstanceEntity> processInstances, String userId) {
        List<ProcessInstanceEntity> content = processInstances.getContent();
        if (content.isEmpty()) {
            return;
        }

        Set<String> pageIds = content.stream().map(ProcessInstanceEntity::getId).collect(Collectors.toSet());

        // One query: all descendants of every page-level process, at any depth (depth > 0 excludes self-rows)
        List<ProcessInstanceHierarchyEntity> hierarchyRows = processInstanceHierarchyRepository.findByAncestorIdInAndDepthGreaterThan(
            pageIds,
            0
        );

        Set<String> descendantIds = hierarchyRows
            .stream()
            .map(ProcessInstanceHierarchyEntity::getDescendantId)
            .collect(Collectors.toSet());

        Map<String, ProcessInstanceEntity> descendantById = fetchDescendants(descendantIds, userId);

        // ancestorId → relationType → set of DTOs
        Map<String, Map<String, Set<QueryCloudSubprocessInstance>>> grouped = hierarchyRows
            .stream()
            .filter(h -> descendantById.containsKey(h.getDescendantId()))
            .collect(
                Collectors.groupingBy(
                    ProcessInstanceHierarchyEntity::getAncestorId,
                    Collectors.groupingBy(
                        ProcessInstanceHierarchyEntity::getRelationType,
                        Collectors.mapping(
                            h -> toSubprocessInstance(descendantById.get(h.getDescendantId())),
                            Collectors.toSet()
                        )
                    )
                )
            );

        content.forEach(pi -> {
            Map<String, Set<QueryCloudSubprocessInstance>> byType = grouped.getOrDefault(pi.getId(), Map.of());
            pi.setSubprocesses(byType.getOrDefault(ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS, Set.of()));
            pi.setLinkedProcesses(byType.getOrDefault(ProcessInstanceHierarchyEntity.RELATION_LINKED, Set.of()));
        });
    }

    private Map<String, ProcessInstanceEntity> fetchDescendants(Set<String> descendantIds, String userId) {
        if (descendantIds.isEmpty()) {
            return Map.of();
        }
        if (userId == null) {
            // Admin: unrestricted batch-fetch
            return StreamSupport
                .stream(processInstanceRepository.findAllById(descendantIds).spliterator(), false)
                .collect(Collectors.toMap(ProcessInstanceEntity::getId, pi -> pi));
        }
        // User: only descendants visible to the requesting user
        ProcessInstanceSearchRequest descendantsRequest = new ProcessInstanceSearchRequest();
        descendantsRequest.setId(descendantIds);
        return processInstanceRepository
            .findAll(ProcessInstanceSpecification.restricted(descendantsRequest, userId))
            .stream()
            .collect(Collectors.toMap(ProcessInstanceEntity::getId, pi -> pi));
    }

    private static QueryCloudSubprocessInstance toSubprocessInstance(ProcessInstanceEntity entity) {
        QueryCloudSubprocessInstance dto = new QueryCloudSubprocessInstance();
        dto.setId(entity.getId());
        dto.setProcessDefinitionName(entity.getProcessDefinitionName());
        return dto;
    }
}
