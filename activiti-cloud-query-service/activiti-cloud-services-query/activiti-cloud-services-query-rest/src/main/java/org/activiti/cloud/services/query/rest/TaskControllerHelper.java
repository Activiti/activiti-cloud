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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.services.query.app.repository.ProcessVariablesPivotRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepositorySpecification;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.ProcessVariableValueFilter;
import org.activiti.cloud.services.query.model.ProcessVariablesPivotEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskSpecifications;
import org.activiti.cloud.services.query.rest.assembler.TaskRepresentationModelAssembler;
import org.activiti.cloud.services.query.rest.dto.TaskDto;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateAggregator;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.hibernate.Filter;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.transaction.annotation.Transactional;

public class TaskControllerHelper {

    private final TaskRepository taskRepository;

    private final TaskRepositorySpecification taskRepositorySpecification;

    private final AlfrescoPagedModelAssembler<TaskDto> pagedCollectionModelAssembler;

    private final QueryDslPredicateAggregator predicateAggregator;

    private final TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    private final TaskLookupRestrictionService taskLookupRestrictionService;

    private final VariableRepository processVariableRepository;

    private final ProcessVariablesPivotRepository processVariablesPivotRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TaskControllerHelper(
        TaskRepository taskRepository,
        TaskRepositorySpecification taskRepositorySpecification,
        AlfrescoPagedModelAssembler<TaskDto> pagedCollectionModelAssembler,
        QueryDslPredicateAggregator predicateAggregator,
        TaskRepresentationModelAssembler taskRepresentationModelAssembler,
        TaskLookupRestrictionService taskLookupRestrictionService,
        VariableRepository processVariableRepository,
        ProcessVariablesPivotRepository processVariablesPivotRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskRepositorySpecification = taskRepositorySpecification;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.predicateAggregator = predicateAggregator;
        this.taskRepresentationModelAssembler = taskRepresentationModelAssembler;
        this.taskLookupRestrictionService = taskLookupRestrictionService;
        this.processVariableRepository = processVariableRepository;
        this.processVariablesPivotRepository = processVariablesPivotRepository;
    }

    public PagedModel<EntityModel<TaskDto>> findAll(
        Predicate predicate,
        VariableSearch variableSearch,
        Pageable pageable,
        List<QueryDslPredicateFilter> filters
    ) {
        Page<TaskEntity> filteredTasks = findPage(predicate, variableSearch, pageable, filters);
        return pagedCollectionModelAssembler.toModel(
            pageable,
            filteredTasks.map(TaskDto::new),
            taskRepresentationModelAssembler
        );
    }

    @Transactional(readOnly = true)
    public PagedModel<EntityModel<TaskDto>> findAllWithProcessVariables(
        Predicate predicate,
        VariableSearch variableSearch,
        Pageable pageable,
        List<QueryDslPredicateFilter> taskFilters,
        Set<ProcessVariableValueFilter> processVariableValueFilters,
        Set<ProcessVariableKey> processVariableFetchKeys
    ) {
        Page<TaskDto> filteredTasks = findPageWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            taskFilters,
            processVariableValueFilters,
            processVariableFetchKeys
        )
            .map(TaskDto::new);
        List<ProcessVariableEntity> processVariables = fetchProcessVariables(
            filteredTasks.getContent(),
            processVariableFetchKeys
        );
        populateProcessVariables(filteredTasks.getContent(), processVariables);
        return pagedCollectionModelAssembler.toModel(pageable, filteredTasks, taskRepresentationModelAssembler);
    }

    public PagedModel<EntityModel<TaskDto>> findAllByInvolvedUserQuery(Predicate predicate, Pageable pageable) {
        Page<TaskDto> page = findAllByInvolvedUser(predicate, pageable).map(TaskDto::new);
        return pagedCollectionModelAssembler.toModel(pageable, page, taskRepresentationModelAssembler);
    }

    @Transactional(readOnly = true)
    public PagedModel<EntityModel<TaskDto>> findAllFromBody(
        Predicate predicate,
        VariableSearch variableSearch,
        Pageable pageable,
        List<QueryDslPredicateFilter> filters,
        Set<ProcessVariableKey> processVariableKeys
    ) {
        if (processVariableKeys == null || processVariableKeys.isEmpty()) {
            return this.findAll(predicate, variableSearch, pageable, filters);
        } else {
            return this.findAllWithProcessVariables(
                    predicate,
                    variableSearch,
                    pageable,
                    filters,
                    Collections.emptySet(),
                    processVariableKeys
                );
        }
    }

    @Transactional(readOnly = true)
    public PagedModel<EntityModel<TaskDto>> findAllByInvolvedUserQueryWithProcessVariables(
        Predicate predicate,
        List<String> processVariableKeys,
        Pageable pageable
    ) {
        addProcessVariablesFilter(processVariableKeys);
        Page<TaskEntity> page = findAllByInvolvedUser(predicate, pageable);
        initializeProcessVariables(page);
        return pagedCollectionModelAssembler.toModel(
            pageable,
            page.map(TaskDto::new),
            taskRepresentationModelAssembler
        );
    }

    private void initializeProcessVariables(Page<TaskEntity> page) {
        page.forEach(taskEntity -> Hibernate.initialize(taskEntity.getProcessVariables()));
    }

    private void addProcessVariablesFilter(List<String> processVariableKeys) {
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("variablesFilter");
        filter.setParameterList("variableKeys", processVariableKeys);
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

    private Page<TaskEntity> findPageWithProcessVariables(
        Predicate predicate,
        VariableSearch variableSearch,
        Pageable pageable,
        List<QueryDslPredicateFilter> filters,
        Set<ProcessVariableValueFilter> processVariableValueFilters,
        Set<ProcessVariableKey> processVariableFetchKeys
    ) {
        Predicate extendedPredicate = predicateAggregator.applyFilters(predicate, filters);

        if (variableSearch.isSet()) {
            addProcessVariablesFilter(
                processVariableFetchKeys
                    .stream()
                    .map(k -> k.processDefinitionKey() + "/" + k.variableName())
                    .collect(Collectors.toList())
            );
            Page<TaskEntity> page = taskRepository.findByVariableNameAndValue(
                variableSearch.getName(),
                variableSearch.getValue(),
                extendedPredicate,
                pageable
            );
            initializeProcessVariables(page);
            return page;
        }

        return taskRepositorySpecification.findAll(
            TaskSpecifications.withDynamicConditions(processVariableValueFilters),
            pageable
        );
    }

    private List<ProcessVariableEntity> fetchProcessVariables(
        Collection<TaskDto> tasks,
        Set<ProcessVariableKey> processVariableFetchKeys
    ) {
        Set<String> processInstanceIds = tasks.stream().map(TaskDto::getProcessInstanceId).collect(Collectors.toSet());
        Iterable<ProcessVariablesPivotEntity> pivot = processVariablesPivotRepository.findAllById(processInstanceIds);
        Set<String> keys = processVariableFetchKeys
            .stream()
            .map(p -> p.processDefinitionKey() + "/" + p.variableName())
            .collect(Collectors.toSet());
        List<ProcessVariableEntity> result = new ArrayList<>();
        pivot.forEach(p ->
            p
                .getValues()
                .forEach((key, value) -> {
                    if (keys.contains(key)) {
                        ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
                        processVariableEntity.setProcessInstanceId(p.getProcessInstanceId());
                        processVariableEntity.setName(key.split("/")[1]);
                        processVariableEntity.setValue(value);
                        result.add(processVariableEntity);
                    }
                })
        );
        return result;
    }

    private void populateProcessVariables(
        Collection<TaskDto> tasks,
        Collection<ProcessVariableEntity> processVariables
    ) {
        Map<String, Map<String, Object>> processVariablesMap = processVariables
            .stream()
            .collect(
                Collectors.groupingBy(
                    ProcessVariableEntity::getProcessInstanceId,
                    Collectors.toMap(ProcessVariableEntity::getName, ProcessVariableEntity::getValue)
                )
            );
        tasks.forEach(task -> task.setProcessVariables(processVariablesMap.get(task.getProcessInstanceId())));
    }
}
