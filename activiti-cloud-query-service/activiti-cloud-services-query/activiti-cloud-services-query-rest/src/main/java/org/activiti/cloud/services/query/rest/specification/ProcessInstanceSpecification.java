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
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import org.activiti.cloud.services.query.app.repository.annotation.CountOverFullWindow;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity_;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity_;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.springframework.util.CollectionUtils;

@CountOverFullWindow
public class ProcessInstanceSpecification
    extends SpecificationSupport<ProcessInstanceEntity, ProcessInstanceSearchRequest> {

    private final String userId;

    private ProcessInstanceSpecification(ProcessInstanceSearchRequest searchRequest, String userId) {
        super(searchRequest);
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
        reset();
        applyUserRestrictionFilter(root, criteriaBuilder);
        applyIdFilter(root);
        applyParentIdFilter(root);
        applyNameFilter(root, criteriaBuilder);
        applyProcessDefinitionNameFilter(root);
        applyInitiatorFilter(root);
        applyAppVersionFilter(root);
        applyStatusFilter(root);
        applyLastModifiedDateFilters(root, criteriaBuilder);
        applyStartFilters(root, criteriaBuilder);
        applyCompletedFilters(root, criteriaBuilder);
        applySuspendedFilters(root, criteriaBuilder);
        return super.toPredicate(root, query, criteriaBuilder);
    }

    @Override
    protected SingularAttribute<ProcessInstanceEntity, String> getIdAttribute() {
        return ProcessInstanceEntity_.id;
    }

    @Override
    protected SetAttribute<ProcessInstanceEntity, ProcessVariableEntity> getProcessVariablesAttribute() {
        return ProcessInstanceEntity_.variables;
    }

    private void applyParentIdFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.parentId())) {
            predicates.add(root.get(ProcessInstanceEntity_.parentId).in(searchRequest.parentId()));
        }
    }

    private void applyNameFilter(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(searchRequest.name())) {
            addLikeFilters(predicates, searchRequest.name(), root, criteriaBuilder, ProcessInstanceEntity_.name);
        }
    }

    private void applyProcessDefinitionNameFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.processDefinitionName())) {
            predicates.add(
                root.get(ProcessInstanceEntity_.processDefinitionName).in(searchRequest.processDefinitionName())
            );
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

    private void applyStatusFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.status())) {
            predicates.add(root.get(ProcessInstanceEntity_.status).in(searchRequest.status()));
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
}
