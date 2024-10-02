package org.activiti.cloud.services.query.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessInstanceSearchRequestBuilder {

    private Set<String> names;
    private Set<String> initiators;
    private Set<String> appVersions;
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

    public ProcessInstanceSearchRequestBuilder withNames(String... names) {
        this.names = Set.of(names);
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
            names,
            initiators,
            appVersions,
            lastModifiedFrom,
            lastModifiedTo,
            startFrom,
            startTo,
            completedFrom,
            completedTo,
            suspendedFrom,
            suspendedTo,
            processVariableFilters,
            processVariableKeys
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
