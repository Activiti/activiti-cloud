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
package org.activiti.cloud.services.security;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import java.util.List;
import jakarta.validation.constraints.NotNull;
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

        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;
        String userId = securityManager.getAuthenticatedUserId();

        Predicate defaultRestrictions = restrictTaskQuery(new BooleanBuilder());

        BooleanExpression userIsInvolved = processInstanceEntity.initiator
            .eq(userId) //is Initiator
            .or(
                taskEntity.processInstanceId.in( //user is Involved in one of the tasks of the Process
                    JPAExpressions.select(taskEntity.processInstanceId).from(taskEntity).where(defaultRestrictions)
                )
            )
            .or(defaultRestrictions); //apply default conditions

        return addAndConditionToPredicate(predicate, userIsInvolved);
    }

    private Predicate restrictTaskQuery(Predicate predicate, QTaskEntity task) {
        if (!restrictionsEnabled) {
            return predicate;
        }

        //get authenticated user
        String userId = securityManager.getAuthenticatedUserId();

        BooleanExpression restriction = null;

        if (userId != null) {
            BooleanExpression isNotAssigned = task.assignee.isNull();
            restriction =
                task.assignee
                    .eq(userId) //user is assignee
                    .or(task.owner.eq(userId)) //user is owner
                    .or(
                        task.taskCandidateUsers
                            .any()
                            .userId.eq(userId) //is candidate user and task is not assigned
                            .and(isNotAssigned)
                    );

            List<String> groups = null;
            if (securityManager != null) {
                groups = securityManager.getAuthenticatedUserGroups();
            }
            if (groups != null && groups.size() > 0) {
                //belongs to candidate group and task is not assigned
                restriction = restriction.or(task.taskCandidateGroups.any().groupId.in(groups).and(isNotAssigned));
            }

            //or there are no candidates set and task is not assigned
            restriction =
                restriction.or(
                    task.taskCandidateUsers.isEmpty().and(task.taskCandidateGroups.isEmpty()).and(isNotAssigned)
                );
        }

        return addAndConditionToPredicate(predicate, restriction);
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
