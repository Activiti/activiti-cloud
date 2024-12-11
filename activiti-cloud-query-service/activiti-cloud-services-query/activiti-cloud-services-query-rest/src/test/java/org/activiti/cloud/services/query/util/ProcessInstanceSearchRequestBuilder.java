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
package org.activiti.cloud.services.query.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessInstanceSearchRequestBuilder {

    private Set<String> ids;
    private Set<String> parentIds;
    private Set<String> names;
    private Set<String> processDefinitionNames;
    private Set<String> initiators;
    private Set<String> appVersions;
    private Set<ProcessInstance.ProcessInstanceStatus> statuses;
    private Date lastModifiedFrom;
    private Date lastModifiedTo;
    private Date startFrom;
    private Date startTo;
    private Date completedFrom;
    private Date completedTo;
    private Date suspendedFrom;
    private Date suspendedTo;
    private Set<VariableFilter> processVariableFilters;
    private Set<ProcessVariableKey> processVariableKeys;
    private CloudRuntimeEntitySort sort;

    public ProcessInstanceSearchRequestBuilder withIds(String... ids) {
        this.ids = Set.of(ids);
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withParentIds(String... parentIds) {
        this.parentIds = Set.of(parentIds);
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withNames(String... names) {
        this.names = Set.of(names);
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withProcessDefinitionNames(String... processDefinitionNames) {
        this.processDefinitionNames = Set.of(processDefinitionNames);
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withInitiators(String... initiators) {
        this.initiators = Set.of(initiators);
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withAppVersions(String... appVersions) {
        this.appVersions = Set.of(appVersions);
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withStatus(ProcessInstance.ProcessInstanceStatus... statuses) {
        this.statuses = Set.of(statuses);
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withLastModifiedFrom(Date lastModifiedFrom) {
        this.lastModifiedFrom = lastModifiedFrom;
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withLastModifiedTo(Date lastModifiedTo) {
        this.lastModifiedTo = lastModifiedTo;
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withStartFrom(Date startFrom) {
        this.startFrom = startFrom;
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withStartTo(Date startTo) {
        this.startTo = startTo;
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withCompletedFrom(Date completedFrom) {
        this.completedFrom = completedFrom;
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withCompletedTo(Date completedTo) {
        this.completedTo = completedTo;
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withSuspendedFrom(Date suspendedFrom) {
        this.suspendedFrom = suspendedFrom;
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withSuspendedTo(Date suspendedTo) {
        this.suspendedTo = suspendedTo;
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withProcessVariableFilters(VariableFilter... processVariableFilters) {
        this.processVariableFilters = Set.of(processVariableFilters);
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withProcessVariableKeys(ProcessVariableKey... processVariableKeys) {
        this.processVariableKeys = Set.of(processVariableKeys);
        return this;
    }

    public ProcessInstanceSearchRequestBuilder withSort(CloudRuntimeEntitySort sort) {
        this.sort = sort;
        return this;
    }

    public ProcessInstanceSearchRequestBuilder invertSort() {
        if (sort != null) {
            sort =
                new CloudRuntimeEntitySort(
                    sort.field(),
                    sort.direction().isAscending() ? Sort.Direction.DESC : Sort.Direction.ASC,
                    sort.isProcessVariable(),
                    sort.processDefinitionKey(),
                    sort.type()
                );
        }
        return this;
    }

    public ProcessInstanceSearchRequest build() {
        if (processVariableFilters != null) {
            Set<ProcessVariableKey> keysFromFilters = processVariableFilters
                .stream()
                .map(variableFilter ->
                    new ProcessVariableKey(variableFilter.processDefinitionKey(), variableFilter.name())
                )
                .collect(Collectors.toSet());
            if (processVariableKeys == null) {
                processVariableKeys = keysFromFilters;
            } else {
                processVariableKeys.addAll(keysFromFilters);
            }
        }
        return new ProcessInstanceSearchRequest(
            ids,
            parentIds,
            names,
            processDefinitionNames,
            initiators,
            appVersions,
            statuses,
            lastModifiedFrom,
            lastModifiedTo,
            startFrom,
            startTo,
            completedFrom,
            completedTo,
            suspendedFrom,
            suspendedTo,
            processVariableFilters,
            processVariableKeys,
            sort
        );
    }

    public String buildJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            return objectMapper.writeValueAsString(build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
