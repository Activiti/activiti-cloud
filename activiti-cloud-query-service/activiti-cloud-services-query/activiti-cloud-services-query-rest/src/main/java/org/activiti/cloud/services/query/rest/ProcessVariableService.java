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
package org.activiti.cloud.services.query.rest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.specification.ProcessVariableSpecification;
import org.springframework.util.CollectionUtils;

public class ProcessVariableService {

    private final VariableRepository variableRepository;

    public ProcessVariableService(VariableRepository variableRepository) {
        this.variableRepository = variableRepository;
    }

    public void fetchProcessVariablesForProcessInstances(
        Collection<ProcessInstanceEntity> processInstances,
        Set<ProcessVariableKey> variableKeys
    ) {
        if (!CollectionUtils.isEmpty(variableKeys)) {
            Set<String> processInstanceIds = processInstances
                .stream()
                .map(ProcessInstanceEntity::getId)
                .collect(Collectors.toSet());

            Map<String, Set<ProcessVariableEntity>> processVariablesMap = fetchVariablesInternal(
                processInstanceIds,
                variableKeys
            );
            processInstances.forEach(pi ->
                pi.setVariables(processVariablesMap.getOrDefault(pi.getId(), Collections.emptySet()))
            );
        } else {
            processInstances.forEach(pi -> pi.setVariables(Collections.emptySet()));
        }
    }

    public void fetchProcessVariablesForTasks(
        Collection<TaskEntity> tasks,
        Collection<String> processVariableFetchKeys
    ) {
        fetchProcessVariablesForTasks(
            tasks,
            processVariableFetchKeys.stream().map(ProcessVariableKey::fromString).collect(Collectors.toSet())
        );
    }

    public void fetchProcessVariablesForTasks(
        Collection<TaskEntity> tasks,
        Set<ProcessVariableKey> processVariableFetchKeys
    ) {
        if (!CollectionUtils.isEmpty(processVariableFetchKeys)) {
            Set<String> processInstanceIds = tasks
                .stream()
                .map(QueryCloudTask::getProcessInstanceId)
                .collect(Collectors.toSet());

            Map<String, Set<ProcessVariableEntity>> processVariablesMap = fetchVariablesInternal(
                processInstanceIds,
                processVariableFetchKeys
            );
            tasks.forEach(task ->
                task.setProcessVariables(
                    processVariablesMap.getOrDefault(task.getProcessInstanceId(), Collections.emptySet())
                )
            );
        } else {
            tasks.forEach(task -> task.setProcessVariables(Collections.emptySet()));
        }
    }

    private Map<String, Set<ProcessVariableEntity>> fetchVariablesInternal(
        Set<String> processInstanceIds,
        Set<ProcessVariableKey> processVariableFetchKeys
    ) {
        List<ProcessVariableEntity> processVariables = variableRepository.findBy(
            new ProcessVariableSpecification(processInstanceIds, processVariableFetchKeys),
            q -> q.project(ProcessVariableEntity_.VALUE).all()
        );
        return processVariables
            .stream()
            .collect(Collectors.groupingBy(ProcessVariableEntity::getProcessInstanceId, Collectors.toSet()));
    }
}
