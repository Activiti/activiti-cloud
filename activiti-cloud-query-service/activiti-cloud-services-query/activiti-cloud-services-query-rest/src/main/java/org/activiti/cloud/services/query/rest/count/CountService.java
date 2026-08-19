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
package org.activiti.cloud.services.query.rest.count;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.rest.ProcessInstanceSearchService;
import org.activiti.cloud.services.query.rest.TaskControllerHelper;
import org.activiti.cloud.services.query.rest.payload.BatchCountRequest;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.rest.payload.ResourceType;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Counts each requested filter per resource type, keyed by the filter's requestId.
 * {@code restricted} selects the per-user vs admin tier.
 */
public class CountService {

    private final TaskControllerHelper taskControllerHelper;
    private final ProcessInstanceSearchService processInstanceSearchService;

    public CountService(
        TaskControllerHelper taskControllerHelper,
        ProcessInstanceSearchService processInstanceSearchService
    ) {
        this.taskControllerHelper = taskControllerHelper;
        this.processInstanceSearchService = processInstanceSearchService;
    }

    @Transactional(readOnly = true)
    public Map<ResourceType, Map<String, Long>> countRestricted(BatchCountRequest request) {
        return count(request, true);
    }

    @Transactional(readOnly = true)
    public Map<ResourceType, Map<String, Long>> countUnrestricted(BatchCountRequest request) {
        return count(request, false);
    }

    private Map<ResourceType, Map<String, Long>> count(BatchCountRequest request, boolean restricted) {
        validate(request);
        Map<ResourceType, Map<String, Long>> results = new EnumMap<>(ResourceType.class);
        if (request.task() != null && !request.task().isEmpty()) {
            results.put(ResourceType.TASK, countTasks(request.task(), restricted));
        }
        if (request.processInstance() != null && !request.processInstance().isEmpty()) {
            results.put(ResourceType.PROCESS_INSTANCE, countProcessInstances(request.processInstance(), restricted));
        }
        return results;
    }

    private Map<String, Long> countTasks(List<TaskSearchRequest> filters, boolean restricted) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TaskSearchRequest filter : filters) {
            counts.put(
                filter.requestId(),
                restricted
                    ? taskControllerHelper.countTasksRestricted(filter)
                    : taskControllerHelper.countTasksUnrestricted(filter)
            );
        }
        return counts;
    }

    private Map<String, Long> countProcessInstances(List<ProcessInstanceSearchRequest> filters, boolean restricted) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ProcessInstanceSearchRequest filter : filters) {
            counts.put(
                filter.getRequestId(),
                restricted
                    ? processInstanceSearchService.countRestricted(filter)
                    : processInstanceSearchService.countUnrestricted(filter)
            );
        }
        return counts;
    }

    private void validate(BatchCountRequest request) {
        int taskFilters = request == null || request.task() == null ? 0 : request.task().size();
        int processInstanceFilters =
            request == null || request.processInstance() == null ? 0 : request.processInstance().size();
        if (taskFilters == 0 && processInstanceFilters == 0) {
            throw new IllegalStateException("At least one resource type must be provided");
        }
        validateRequestIds(request);
    }

    private void validateRequestIds(BatchCountRequest request) {
        Set<String> taskRequestIds = new HashSet<>();
        if (request.task() != null) {
            for (TaskSearchRequest filter : request.task()) {
                checkRequestId(filter.requestId(), taskRequestIds, ResourceType.TASK);
            }
        }
        Set<String> processInstanceRequestIds = new HashSet<>();
        if (request.processInstance() != null) {
            for (ProcessInstanceSearchRequest filter : request.processInstance()) {
                checkRequestId(filter.getRequestId(), processInstanceRequestIds, ResourceType.PROCESS_INSTANCE);
            }
        }
    }

    private void checkRequestId(String requestId, Set<String> seen, ResourceType resourceType) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException(
                "Each count filter for resource type " + resourceType + " must specify a requestId"
            );
        }
        if (!seen.add(requestId)) {
            throw new IllegalStateException(
                "Duplicate requestId '" + requestId + "' for resource type " + resourceType
            );
        }
    }
}
