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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.rest.specification.ProcessInstanceSpecification;
import org.activiti.cloud.services.query.rest.specification.SubqueryWrappingSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class ProcessInstanceSearchService {

    private final ProcessInstanceRepository processInstanceRepository;

    private final ProcessVariableService processVariableService;

    private final SecurityManager securityManager;

    @PersistenceContext
    private EntityManager entityManager;

    public ProcessInstanceSearchService(
        ProcessInstanceRepository processInstanceRepository,
        ProcessVariableService processVariableService,
        SecurityManager securityManager
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.processVariableService = processVariableService;
        this.securityManager = securityManager;
    }

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> searchRestricted(ProcessInstanceSearchRequest searchRequest, Pageable pageable) {
        return search(
            searchRequest.processVariableKeys(),
            pageable,
            ProcessInstanceSpecification.restricted(searchRequest, securityManager.getAuthenticatedUserId())
        );
    }

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> searchUnrestricted(
        ProcessInstanceSearchRequest searchRequest,
        Pageable pageable
    ) {
        return search(
            searchRequest.processVariableKeys(),
            pageable,
            ProcessInstanceSpecification.unrestricted(searchRequest)
        );
    }

    /**
     * @param processVariableKeys the process variables to fetch for each process instance, each represented by process definition key and variable name
     * @param pageable the page request. N.B. the sort contained in this pageable will be ignored and the sort from the search request will be used instead
     * @param specification the specification to use for the search. It includes the sorting parameter.
     * @return the page of process instances
     */
    private Page<ProcessInstanceEntity> search(
        Set<ProcessVariableKey> processVariableKeys,
        Pageable pageable,
        ProcessInstanceSpecification specification
    ) {
        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(
            executeTupleQueryAndExtractTasks(getTupleQuery(specification, pageable)),
            pageable,
            processInstanceRepository.count(new SubqueryWrappingSpecification<>(specification))
        );
        processVariableService.fetchProcessVariablesForProcessInstances(
            processInstances.getContent(),
            processVariableKeys
        );
        return processInstances;
    }

    private TypedQuery<Tuple> getTupleQuery(ProcessInstanceSpecification taskSpecification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> tupleQuery = cb.createTupleQuery();
        Root<ProcessInstanceEntity> root = tupleQuery.from(ProcessInstanceEntity.class);
        tupleQuery.where(taskSpecification.toPredicate(root, tupleQuery, cb));
        List<Selection<?>> selections = new ArrayList<>();
        selections.add(root);
        tupleQuery.getOrderList().forEach(order -> selections.add(order.getExpression()));
        tupleQuery.multiselect(selections.toArray(new Selection[0]));
        TypedQuery<Tuple> query = entityManager.createQuery(tupleQuery);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return query;
    }

    private List<ProcessInstanceEntity> executeTupleQueryAndExtractTasks(TypedQuery<Tuple> query) {
        return query
            .getResultList()
            .stream()
            .map(t -> t.get(0, ProcessInstanceEntity.class))
            .collect(Collectors.toList());
    }
}
