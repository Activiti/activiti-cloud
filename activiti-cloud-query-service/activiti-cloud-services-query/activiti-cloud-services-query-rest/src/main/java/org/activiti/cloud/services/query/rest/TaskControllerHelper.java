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

import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Collections;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.ProcessVariableSpecification;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.assembler.TaskRepresentationModelAssembler;
import org.activiti.cloud.services.query.rest.dto.ProcessVariableDto;
import org.activiti.cloud.services.query.rest.dto.TaskDto;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateAggregator;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.activiti.cloud.services.query.rest.specification.ProcessVariableSpecification;
import org.activiti.cloud.services.query.rest.specification.TaskSpecification;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.transaction.annotation.Transactional;

public class TaskControllerHelper {

    private final TaskRepository taskRepository;

    private final VariableRepository processVariableRepository;

    private final AlfrescoPagedModelAssembler<TaskDto> pagedCollectionModelAssembler;

    private final QueryDslPredicateAggregator predicateAggregator;

    private final TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    private final TaskLookupRestrictionService taskLookupRestrictionService;

    public TaskControllerHelper(
        TaskRepository taskRepository,
        VariableRepository processVariableRepository,
        AlfrescoPagedModelAssembler<TaskEntity> pagedCollectionModelAssembler,
        QueryDslPredicateAggregator predicateAggregator,
        TaskRepresentationModelAssembler taskRepresentationModelAssembler,
        TaskLookupRestrictionService taskLookupRestrictionService
    ) {
        this.taskRepository = taskRepository;
        this.processVariableRepository = processVariableRepository;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.predicateAggregator = predicateAggregator;
        this.taskRepresentationModelAssembler = taskRepresentationModelAssembler;
        this.taskLookupRestrictionService = taskLookupRestrictionService;
    }

    public PagedModel<EntityModel<QueryCloudTask>> findAll(
        Predicate predicate,
        VariableSearch variableSearch,
        Pageable pageable,
        List<QueryDslPredicateFilter> filters
    ) {
        Page<TaskEntity> page = findPage(predicate, variableSearch, pageable, filters);
        return pagedCollectionModelAssembler.toModel(pageable, page, taskRepresentationModelAssembler);
    }

    @Transactional(readOnly = true)
    public PagedModel<EntityModel<QueryCloudTask>> findAllWithProcessVariables(
        Predicate predicate,
        VariableSearch variableSearch,
        Pageable pageable,
        List<QueryDslPredicateFilter> filters,
        List<String> processVariableKeys
    ) {
        Page<TaskEntity> page = findPage(predicate, variableSearch, pageable, filters);
        fetchProcessVariables(page.getContent(), processVariableKeys);
        return pagedCollectionModelAssembler.toModel(
            pageable,
            page.map(TaskDto::new),
            taskRepresentationModelAssembler
        );
    }

    public PagedModel<EntityModel<TaskDto>> searchTasks(TaskSearchRequest taskSearchRequest, Pageable pageable) {
        Page<TaskDto> tasks = taskRepository
            .findAll(new TaskSpecification(taskSearchRequest), pageable)
            .map(TaskDto::new);
        List<ProcessVariableEntity> processVariables = fetchProcessVariables(
            tasks.getContent(),
            taskSearchRequest.processVariableKeys()
        );
        populateProcessVariables(tasks.getContent(), processVariables);
        return pagedCollectionModelAssembler.toModel(pageable, tasks, taskRepresentationModelAssembler);
    }

    public PagedModel<EntityModel<QueryCloudTask>> findAllByInvolvedUserQuery(Predicate predicate, Pageable pageable) {
        Page<TaskEntity> page = findAllByInvolvedUser(predicate, pageable);
        return pagedCollectionModelAssembler.toModel(pageable, page, taskRepresentationModelAssembler);
    }

    @Transactional(readOnly = true)
    public PagedModel<EntityModel<QueryCloudTask>> findAllFromBody(
        Predicate predicate,
        VariableSearch variableSearch,
        Pageable pageable,
        List<QueryDslPredicateFilter> filters,
        List<String> processVariableKeys
    ) {
        if (processVariableKeys == null || processVariableKeys.isEmpty()) {
            return this.findAll(predicate, variableSearch, pageable, filters);
        } else {
            return this.findAllWithProcessVariables(predicate, variableSearch, pageable, filters, processVariableKeys);
        }
    }

    @Transactional(readOnly = true)
    public PagedModel<EntityModel<QueryCloudTask>> findAllByInvolvedUserQueryWithProcessVariables(
        Predicate predicate,
        List<String> processVariableKeys,
        Pageable pageable
    ) {
        Page<TaskEntity> page = findAllByInvolvedUser(predicate, pageable);
        fetchProcessVariables(page.getContent(), processVariableKeys);
        return pagedCollectionModelAssembler.toModel(pageable, page, taskRepresentationModelAssembler);
    }

    private Page<TaskEntity> findAllByInvolvedUser(Predicate predicate, Pageable pageable) {
        Predicate conditions = taskLookupRestrictionService.restrictToInvolvedUsersQuery(predicate);
        return taskRepository.findInProcessInstanceScope(conditions, pageable);
    }

    public boolean canUserViewTask(Predicate predicate) {
        Predicate conditions = taskLookupRestrictionService.restrictToInvolvedUsersQuery(predicate);
        return taskRepository.existsInProcessInstanceScope(conditions);
    }

    private Page<TaskEntity> findPage(
        Predicate predicate,
        VariableSearch variableSearch,
        Pageable pageable,
        List<QueryDslPredicateFilter> filters
    ) {
        Predicate extendedPredicate = predicateAggregator.applyFilters(predicate, filters);

        Page<TaskEntity> page;
        if (variableSearch.isSet()) {
            page =
                taskRepository.findByVariableNameAndValue(
                    variableSearch.getName(),
                    variableSearch.getValue(),
                    extendedPredicate,
                    pageable
                );
        } else {
            page = taskRepository.findAll(extendedPredicate, pageable);
        }
        return page;
    }

    private void fetchProcessVariables(Collection<TaskEntity> tasks, List<String> processVariableKeys) {
        if (processVariableKeys.isEmpty()) {
            tasks.forEach(task -> task.setProcessVariables(Collections.emptySet()));
        } else {
            Set<String> processInstanceIds = tasks
                .stream()
                .map(TaskEntity::getProcessInstanceId)
                .collect(Collectors.toSet());
            var processVariables = processVariableRepository.findAll(
                new ProcessVariableSpecification(
                    processInstanceIds,
                    processVariableKeys.stream().map(ProcessVariableKey::fromString).collect(Collectors.toSet())
                )
            );

            Map<String, Set<ProcessVariableEntity>> processVariablesMap = processVariables
                .stream()
                .collect(
                    Collectors.groupingBy(
                        ProcessVariableEntity::getProcessInstanceId,
                        Collectors.mapping(pv -> pv, Collectors.toSet())
                    )
                );
            tasks.forEach(task ->
                task.setProcessVariables(
                    processVariablesMap.getOrDefault(task.getProcessInstanceId(), Collections.emptySet())
                )
            );
        }
    }
}
