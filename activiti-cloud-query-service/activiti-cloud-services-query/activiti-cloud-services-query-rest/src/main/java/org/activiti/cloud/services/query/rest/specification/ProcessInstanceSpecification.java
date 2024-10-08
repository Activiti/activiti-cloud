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
package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.dialect.CustomPostgreSQLDialect;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity_;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity_;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSort;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

public class ProcessInstanceSpecification extends SpecificationSupport<ProcessInstanceEntity> {

    List<Predicate> predicates = new ArrayList<>();

    private final String userId;

    private final ProcessInstanceSearchRequest searchRequest;

    private final Sort sort;

    private ProcessInstanceSpecification(ProcessInstanceSearchRequest searchRequest, String userId, Sort sort) {
        this.searchRequest = searchRequest;
        this.userId = userId;
        this.sort = sort;
    }

    public static ProcessInstanceSpecification unrestricted(ProcessInstanceSearchRequest searchRequest, Sort sort) {
        return new ProcessInstanceSpecification(searchRequest, null, sort);
    }

    public static ProcessInstanceSpecification restricted(
        ProcessInstanceSearchRequest searchRequest,
        String userId,
        Sort sort
    ) {
        return new ProcessInstanceSpecification(searchRequest, userId, sort);
    }

    @Override
    public Predicate toPredicate(
        Root<ProcessInstanceEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        applyUserRestrictionFilter(root, criteriaBuilder);
        applyNameFilter(root, criteriaBuilder);
        applyInitiatorFilter(root);
        applyAppVersionFilter(root);
        applyLastModifiedDateFilters(root, criteriaBuilder);
        applyStartFilters(root, criteriaBuilder);
        applyCompletedFilters(root, criteriaBuilder);
        applySuspendedFilters(root, criteriaBuilder);
        applyProcessVariableFilters(root, query, criteriaBuilder);
        applySorting(root, query, criteriaBuilder);
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    private void applySorting(
        Root<ProcessInstanceEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        if (searchRequest.sort() != null) {
            validateSort(searchRequest.sort());
            ProcessInstanceSort sort = searchRequest.sort();
            Expression<Object> orderByClause;
            if (sort.isProcessVariable()) {
                SetJoin<ProcessInstanceEntity, ProcessVariableEntity> joinRoot = root.join(
                    ProcessInstanceEntity_.variables,
                    JoinType.LEFT
                );
                Expression<?> extractedValue = criteriaBuilder.function(
                    CustomPostgreSQLDialect.getExtractionFunction(sort.type()),
                    sort.type().getJavaType(),
                    joinRoot.get(ProcessVariableEntity_.value)
                );
                orderByClause =
                    criteriaBuilder
                        .selectCase()
                        .when(
                            criteriaBuilder.and(
                                criteriaBuilder.equal(
                                    joinRoot.get(ProcessVariableEntity_.processDefinitionKey),
                                    sort.processDefinitionKey()
                                ),
                                criteriaBuilder.equal(joinRoot.get(ProcessVariableEntity_.name), sort.field())
                            ),
                            extractedValue
                        )
                        .otherwise(criteriaBuilder.nullLiteral(Object.class));
            } else {
                orderByClause = root.get(sort.field());
            }
            if (sort.direction().isAscending()) {
                query.orderBy(criteriaBuilder.asc(orderByClause));
            } else {
                //This is a workaround to override the nulls first behavior when ordering direction is DESC
                query.orderBy(criteriaBuilder.asc(orderByClause.isNull()), criteriaBuilder.desc(orderByClause));
            }
        }
    }

    private void validateSort(ProcessInstanceSort sort) {
        if (sort.isProcessVariable()) {
            if (sort.processDefinitionKey() == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Process definition key is required when sorting by process variable"
                );
            }
            if (sort.type() == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Variable type is required when sorting by process variable"
                );
            }
        }
    }

    private void applyNameFilter(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(searchRequest.name())) {
            addLikeFilters(predicates, searchRequest.name(), root, criteriaBuilder, ProcessInstanceEntity_.name);
        }
    }

    private void applyInitiatorFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.initiator())) {
            predicates.add(root.get(ProcessInstanceEntity_.initiator).in(searchRequest.initiator()));
        }
    }

    private void applyAppVersionFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.appVersion())) {
            predicates.add(root.get(ProcessInstanceEntity_.appVersion).in(searchRequest.appVersion()));
        }
    }

    private void applyLastModifiedDateFilters(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.lastModifiedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(ProcessInstanceEntity_.lastModified),
                    searchRequest.lastModifiedFrom()
                )
            );
        }
        if (searchRequest.lastModifiedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(ProcessInstanceEntity_.lastModified), searchRequest.lastModifiedTo())
            );
        }
    }

    private void applyStartFilters(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.startFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(ProcessInstanceEntity_.startDate), searchRequest.startFrom())
            );
        }
        if (searchRequest.startTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(ProcessInstanceEntity_.startDate), searchRequest.startTo())
            );
        }
    }

    private void applyCompletedFilters(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.completedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(ProcessInstanceEntity_.completedDate),
                    searchRequest.completedFrom()
                )
            );
        }
        if (searchRequest.completedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(ProcessInstanceEntity_.completedDate), searchRequest.completedTo())
            );
        }
    }

    private void applySuspendedFilters(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.suspendedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(ProcessInstanceEntity_.suspendedDate),
                    searchRequest.suspendedFrom()
                )
            );
        }
        if (searchRequest.suspendedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(ProcessInstanceEntity_.suspendedDate), searchRequest.suspendedTo())
            );
        }
    }

    private void applyUserRestrictionFilter(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (userId != null) {
            predicates.add(
                criteriaBuilder.or(
                    criteriaBuilder.equal(root.get(ProcessInstanceEntity_.initiator), userId),
                    criteriaBuilder.equal(
                        root.join(ProcessInstanceEntity_.tasks, JoinType.LEFT).get(TaskEntity_.assignee),
                        userId
                    ),
                    criteriaBuilder.equal(
                        root
                            .join(ProcessInstanceEntity_.tasks, JoinType.LEFT)
                            .join(TaskEntity_.taskCandidateUsers, JoinType.LEFT)
                            .get(TaskCandidateUserEntity_.userId),
                        userId
                    )
                )
            );
        }
    }

    private void applyProcessVariableFilters(
        Root<ProcessInstanceEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        if (!CollectionUtils.isEmpty(searchRequest.processVariableFilters())) {
            Root<ProcessVariableEntity> pvRoot = query.from(ProcessVariableEntity.class);
            Predicate joinCondition = criteriaBuilder.equal(
                root.get(ProcessInstanceEntity_.id),
                pvRoot.get(ProcessVariableEntity_.processInstanceId)
            );

            Predicate[] variableValueFilters = getProcessVariableValueFilters(
                pvRoot,
                searchRequest.processVariableFilters(),
                criteriaBuilder
            );

            query.groupBy(root.get(ProcessInstanceEntity_.id));
            query.having(getHavingClause(pvRoot, searchRequest.processVariableFilters(), criteriaBuilder));
            predicates.add(criteriaBuilder.and(joinCondition, criteriaBuilder.or(variableValueFilters)));
        }
    }
}
