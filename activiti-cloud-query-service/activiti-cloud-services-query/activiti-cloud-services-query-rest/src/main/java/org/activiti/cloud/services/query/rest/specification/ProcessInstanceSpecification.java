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
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity_;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity_;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.springframework.util.CollectionUtils;

public class ProcessInstanceSpecification extends SpecificationSupport<ProcessInstanceEntity> {

    List<Predicate> predicates = new ArrayList<>();

    private final String userId;

    private final ProcessInstanceSearchRequest searchRequest;

    private ProcessInstanceSpecification(ProcessInstanceSearchRequest searchRequest, String userId) {
        this.searchRequest = searchRequest;
        this.userId = userId;
    }

    public static ProcessInstanceSpecification unrestricted(ProcessInstanceSearchRequest searchRequest) {
        return new ProcessInstanceSpecification(searchRequest, null);
    }

    public static ProcessInstanceSpecification restricted(ProcessInstanceSearchRequest searchRequest, String userId) {
        return new ProcessInstanceSpecification(searchRequest, userId);
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
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
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
            query.having(
                criteriaBuilder.equal(
                    criteriaBuilder.countDistinct(pvRoot.get(ProcessVariableEntity_.name)),
                    criteriaBuilder.literal(searchRequest.processVariableFilters().size())
                )
            );
            predicates.add(criteriaBuilder.and(joinCondition, criteriaBuilder.or(variableValueFilters)));
        }
    }
}
