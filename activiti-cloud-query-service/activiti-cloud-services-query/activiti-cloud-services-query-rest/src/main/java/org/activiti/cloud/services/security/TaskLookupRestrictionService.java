/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.security;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.springframework.beans.factory.annotation.Value;

/*
 * Tested by RestrictTaskQueryIT
 * Applies permissions/restrictions to TaskEntity data (and TaskEntity Variables) based upon Candidate user/group logic
 */
public class TaskLookupRestrictionService implements QueryDslPredicateFilter {

    private final SecurityManager securityManager;

    @Value("${activiti.cloud.security.task.restrictions.enabled:true}")
    private boolean restrictionsEnabled;

    @Value("${activiti.cloud.security.task.restrictions.involved.user.enabled:true}")
    private boolean restrictionsInvolvedUserEnabled;

    public TaskLookupRestrictionService(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public Predicate restrictTaskQuery(Predicate predicate) {
        return restrictTaskQuery(predicate, QTaskEntity.taskEntity);
    }

    @Override
    public Predicate extend(@NotNull Predicate currentPredicate) {
        return restrictTaskQuery(currentPredicate);
    }

    public Predicate restrictTaskVariableQuery(Predicate predicate) {
        QTaskEntity task = QTaskVariableEntity.taskVariableEntity.task;

        Predicate extendedPredicate = addAndConditionToPredicate(predicate, task.isNotNull());

        return restrictTaskQuery(extendedPredicate, task);
    }

    public Predicate restrictToInvolvedUsersQuery(Predicate predicate) {
        if (!restrictionsInvolvedUserEnabled) {
            return restrictTaskQuery(predicate);
        }

        BooleanExpression userIsInvolved = isProcessInitiator()
            .or(hasVisibleTaskInSameProcessInstance())
            .or(restrictTaskQuery(new BooleanBuilder()));

        return addAndConditionToPredicate(predicate, userIsInvolved);
    }

    private BooleanExpression isProcessInitiator() {
        String userId = securityManager.getAuthenticatedUserId();
        return QProcessInstanceEntity.processInstanceEntity.initiator.eq(userId);
    }

    private BooleanExpression hasVisibleTaskInSameProcessInstance() {
        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        QTaskEntity candidateTask = new QTaskEntity("candidateTask");
        Predicate candidateTaskRestrictions = restrictTaskQuery(new BooleanBuilder(), candidateTask);

        return JPAExpressions.selectOne()
            .from(candidateTask)
            .where(candidateTask.processInstanceId.eq(taskEntity.processInstanceId).and(candidateTaskRestrictions))
            .exists();
    }

    private Predicate restrictTaskQuery(Predicate predicate, QTaskEntity task) {
        if (!restrictionsEnabled) {
            return predicate;
        }

        String userId = securityManager.getAuthenticatedUserId();
        BooleanExpression restriction = userId != null ? buildUserVisibilityRestriction(task, userId) : null;

        return addAndConditionToPredicate(predicate, restriction);
    }

    private BooleanExpression buildUserVisibilityRestriction(QTaskEntity task, String userId) {
        BooleanExpression isNotAssigned = task.assignee.isNull();

        BooleanExpression restriction = isAssignee(task, userId)
            .or(isOwner(task, userId))
            .or(isCandidateUser(task, userId).and(isNotAssigned));

        List<String> groups = securityManager != null ? securityManager.getAuthenticatedUserGroups() : null;
        if (groups != null && groups.size() > 0) {
            restriction = restriction.or(isCandidateGroupMember(task, groups).and(isNotAssigned));
        }

        return restriction.or(hasNoCandidates(task).and(isNotAssigned));
    }

    private BooleanExpression isAssignee(QTaskEntity task, String userId) {
        return task.assignee.eq(userId);
    }

    private BooleanExpression isOwner(QTaskEntity task, String userId) {
        return task.owner.eq(userId);
    }

    private BooleanExpression isCandidateUser(QTaskEntity task, String userId) {
        return task.taskCandidateUsers.any().userId.eq(userId);
    }

    private BooleanExpression isCandidateGroupMember(QTaskEntity task, List<String> groups) {
        return task.taskCandidateGroups.any().groupId.in(groups);
    }

    private BooleanExpression hasNoCandidates(QTaskEntity task) {
        return task.taskCandidateUsers.isEmpty().and(task.taskCandidateGroups.isEmpty());
    }

    private Predicate addAndConditionToPredicate(Predicate predicate, BooleanExpression expression) {
        if (expression != null && predicate != null) {
            return expression.and(predicate);
        }
        if (expression == null) {
            return predicate;
        }
        return expression;
    }

    public void setRestrictionsEnabled(boolean restrictionsEnabled) {
        this.restrictionsEnabled = restrictionsEnabled;
    }

    public boolean isRestrictionsEnabled() {
        return restrictionsEnabled;
    }

    public boolean isRestrictionsInvolvedUserEnabled() {
        return restrictionsInvolvedUserEnabled;
    }

    public void setRestrictionsInvolvedUserEnabled(boolean restrictionsInvolvedUserEnabled) {
        this.restrictionsInvolvedUserEnabled = restrictionsInvolvedUserEnabled;
    }
}
