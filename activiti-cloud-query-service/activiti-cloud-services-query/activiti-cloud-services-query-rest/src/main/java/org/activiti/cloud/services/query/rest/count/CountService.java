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

/**
 * Counts each requested filter per resource type, keyed by the filter's status.
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

    public Map<ResourceType, Map<String, Long>> countRestricted(BatchCountRequest request) {
        return count(request, true);
    }

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
            String key = statusKey(filter.status(), ResourceType.TASK);
            checkDuplicate(counts, key, ResourceType.TASK);
            counts.put(
                key,
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
            String key = statusKey(filter.getStatus(), ResourceType.PROCESS_INSTANCE);
            checkDuplicate(counts, key, ResourceType.PROCESS_INSTANCE);
            counts.put(
                key,
                restricted
                    ? processInstanceSearchService.countRestricted(filter)
                    : processInstanceSearchService.countUnrestricted(filter)
            );
        }
        return counts;
    }

    private void checkDuplicate(Map<String, Long> counts, String key, ResourceType resourceType) {
        if (counts.containsKey(key)) {
            throw new IllegalStateException("Duplicate status filter '" + key + "' for resource type " + resourceType);
        }
    }

    private String statusKey(Set<? extends Enum<?>> statuses, ResourceType resourceType) {
        if (statuses == null || statuses.size() != 1) {
            throw new IllegalStateException(
                "Each count filter for resource type " + resourceType + " must specify exactly one status"
            );
        }
        return statuses.iterator().next().name();
    }

    private void validate(BatchCountRequest request) {
        int taskFilters = request == null || request.task() == null ? 0 : request.task().size();
        int processInstanceFilters =
            request == null || request.processInstance() == null ? 0 : request.processInstance().size();
        if (taskFilters == 0 && processInstanceFilters == 0) {
            throw new IllegalStateException("At least one resource type must be provided");
        }
    }
}
