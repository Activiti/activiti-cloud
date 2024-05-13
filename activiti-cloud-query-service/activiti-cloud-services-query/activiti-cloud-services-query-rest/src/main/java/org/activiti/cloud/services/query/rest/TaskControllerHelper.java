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
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.assembler.TaskRepresentationModelAssembler;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateAggregator;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.hibernate.Filter;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.transaction.annotation.Transactional;

public class TaskControllerHelper {

    private final TaskRepository taskRepository;
    private final AlfrescoPagedModelAssembler<TaskEntity> pagedCollectionModelAssembler;
    private final QueryDslPredicateAggregator predicateAggregator;
    private final TaskRepresentationModelAssembler taskRepresentationModelAssembler;
    private final TaskLookupRestrictionService taskLookupRestrictionService;

    @PersistenceContext
    private EntityManager entityManager;

    public TaskControllerHelper(
        TaskRepository taskRepository,
        AlfrescoPagedModelAssembler<TaskEntity> pagedCollectionModelAssembler,
        QueryDslPredicateAggregator predicateAggregator,
        TaskRepresentationModelAssembler taskRepresentationModelAssembler,
        TaskLookupRestrictionService taskLookupRestrictionService
    ) {
        this.taskRepository = taskRepository;
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
        addProcessVariablesFilter(processVariableKeys);
        Page<TaskEntity> page = findPage(predicate, variableSearch, pageable, filters);
        return pagedCollectionModelAssembler.toModel(pageable, page, taskRepresentationModelAssembler);
    }

    public PagedModel<EntityModel<QueryCloudTask>> findAllByInvolvedUserQuery(Predicate predicate, Pageable pageable) {
        Page<TaskEntity> page = findAllByInvolvedUser(predicate, pageable);
        return pagedCollectionModelAssembler.toModel(pageable, page, taskRepresentationModelAssembler);
    }

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

    @Transactional
    public PagedModel<EntityModel<QueryCloudTask>> findAllByInvolvedUserQueryWithProcessVariables(
        Predicate predicate,
        List<String> processVariableKeys,
        Pageable pageable
    ) {
        addProcessVariablesFilter(processVariableKeys);
        Page<TaskEntity> page = findAllByInvolvedUser(predicate, pageable);
        initializeProcessVariables(page);
        return pagedCollectionModelAssembler.toModel(pageable, page, taskRepresentationModelAssembler);
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
            JPAQuery<TaskEntity> query = new JPAQuery<>(entityManager);
            query.from(QTaskEntity.taskEntity).where(extendedPredicate);
            query.offset(pageable.getOffset());
            query.limit(pageable.getPageSize());
            List<String> taskIds = query.select(QTaskEntity.taskEntity.id).fetch();
            long count = taskRepository.findBy(extendedPredicate, FluentQuery.FetchableFluentQuery::count);

            List<TaskEntity> results = taskRepository.findAllByIdIn(taskIds);
            page = new PageImpl<>(results, pageable, count);
        }
        return page;
    }
}
