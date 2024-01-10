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

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateAggregator;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;

public class ProcessInstanceAdminService {

    private final ProcessInstanceRepository processInstanceRepository;

    private final EntityFinder entityFinder;

    private final QueryDslPredicateAggregator predicateAggregator;

    @PersistenceContext
    private EntityManager entityManager;

    public ProcessInstanceAdminService(
        ProcessInstanceRepository processInstanceRepository,
        EntityFinder entityFinder,
        QueryDslPredicateAggregator queryDslPredicateAggregator
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.entityFinder = entityFinder;
        this.predicateAggregator = queryDslPredicateAggregator;
    }

    public Page<ProcessInstanceEntity> findAll(Predicate predicate, Pageable pageable) {
        return processInstanceRepository.findAll(
            Optional.ofNullable(predicate).orElseGet(BooleanBuilder::new),
            pageable
        );
    }

    public Page<ProcessInstanceEntity> findAllFromBody(
        Predicate predicate,
        List<String> variableKeys,
        List<QueryDslPredicateFilter> filters,
        Pageable pageable
    ) {
        Predicate extendedPredicate = predicateAggregator.applyFilters(predicate, filters);
        if (variableKeys == null || variableKeys.isEmpty()) {
            return this.findAll(extendedPredicate, pageable);
        } else {
            return this.findAllWithVariables(extendedPredicate, variableKeys, pageable);
        }
    }

    @Transactional
    public Page<ProcessInstanceEntity> findAllWithVariables(
        Predicate predicate,
        List<String> variableKeys,
        Pageable pageable
    ) {
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("variablesFilter");
        filter.setParameterList("variableKeys", variableKeys);
        Page<ProcessInstanceEntity> processInstanceEntities = findAll(predicate, pageable);
        var ids = processInstanceEntities.map(ProcessInstanceEntity::getId).toList();
        var result = processInstanceRepository.findByIdIsIn(ids);

        return new PageImpl<>(result, pageable, processInstanceEntities.getTotalElements());
    }

    public ProcessInstanceEntity findById(@PathVariable String processInstanceId) {
        return entityFinder.findById(
            processInstanceRepository,
            processInstanceId,
            "Unable to find task for the given id:'" + processInstanceId + "'"
        );
    }
}
