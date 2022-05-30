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
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.assembler.TaskRepresentationModelAssembler;
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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

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
        TaskLookupRestrictionService taskLookupRestrictionService) {
        this.taskRepository = taskRepository;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.predicateAggregator = predicateAggregator;
        this.taskRepresentationModelAssembler = taskRepresentationModelAssembler;
        this.taskLookupRestrictionService = taskLookupRestrictionService;
    }

    public PagedModel<EntityModel<QueryCloudTask>> findAll(Predicate predicate,
        VariableSearch variableSearch, Pageable pageable, List<QueryDslPredicateFilter> filters) {
        Predicate extendedPredicate = predicateAggregator.applyFilters(predicate, filters);

        Page<TaskEntity> page;
        if (variableSearch.isSet()) {
            page = taskRepository
                .findByVariableNameAndValue(variableSearch.getName(), variableSearch.getValue(),
                    extendedPredicate,
                    pageable);
        } else {
            page = taskRepository.findAll(extendedPredicate, pageable);
        }

        return pagedCollectionModelAssembler.toModel(pageable,
            page,
            taskRepresentationModelAssembler);
    }

    public PagedModel<EntityModel<QueryCloudTask>> findAllWithProcessVariables(Predicate predicate,
                                                                               VariableSearch variableSearch, Pageable pageable, List<QueryDslPredicateFilter> filters, List<String> processVariableDefinitions) {
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("variableDefinitionIds");
        filter.setParameterList("variables", processVariableDefinitions);

        Predicate extendedPredicate = predicateAggregator.applyFilters(predicate, filters);

        Page<TaskEntity> page;
        if (variableSearch.isSet()) {
            page = taskRepository
                .findByVariableNameAndValue(variableSearch.getName(), variableSearch.getValue(),
                    extendedPredicate,
                    pageable);
        } else {
            page = taskRepository.findAll(extendedPredicate, pageable);
        }

        page.forEach(taskEntity -> Hibernate.initialize(taskEntity.getProcessVariables()));

        return pagedCollectionModelAssembler.toModel(pageable, page, taskRepresentationModelAssembler);
    }

    public PagedModel<EntityModel<QueryCloudTask>> findAllByInvolvedUserQuery(Predicate predicate,
                                                                     Pageable pageable) {

        Predicate conditions = taskLookupRestrictionService.restrictToInvolvedUsersQuery(predicate);
        Page<TaskEntity> page = taskRepository.findInProcessInstanceScope(conditions, pageable);

        return pagedCollectionModelAssembler.toModel(pageable,
                                                     page,
                                                     taskRepresentationModelAssembler);
    }

    public boolean canUserViewTask(Predicate predicate) {
        Predicate conditions = taskLookupRestrictionService.restrictToInvolvedUsersQuery(predicate);
        return taskRepository.existsInProcessInstanceScope(conditions);
    }

}
