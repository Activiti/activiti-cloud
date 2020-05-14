/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.security;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/*
 * Tested by RestrictTaskQueryIT
 * Applies permissions/restrictions to TaskEntity data (and TaskEntity Variables) based upon Candidate user/group logic
 */
public class TaskLookupRestrictionService {

    private final SecurityManager securityManager;

    @Value("${activiti.cloud.security.task.restrictions.enabled:true}")
    private boolean restrictionsEnabled;

    public TaskLookupRestrictionService(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public Predicate restrictTaskQuery(Predicate predicate){

        return restrictTaskQuery(predicate, QTaskEntity.taskEntity);
    }


    public Predicate restrictTaskVariableQuery(Predicate predicate){

        QTaskEntity task = QTaskVariableEntity.taskVariableEntity.task;

        Predicate extendedPredicate = addAndConditionToPredicate(predicate,task.isNotNull());

        return restrictTaskQuery(extendedPredicate, task);
    }

    private Predicate restrictTaskQuery(Predicate predicate, QTaskEntity task){

        if (!restrictionsEnabled){
            return predicate;
        }

        //get authenticated user
        String userId = securityManager.getAuthenticatedUserId();

        BooleanExpression restriction = null;

        if(userId!=null) {

            BooleanExpression isNotAssigned = task.assignee.isNull();
            restriction = task.assignee.eq(userId) //user is assignee
                    .or(task.owner.eq(userId)) //user is owner
                    .or(task.taskCandidateUsers.any().userId.eq(userId) //is candidate user and task is not assigned
                                .and(isNotAssigned));


            List<String> groups = null;
            if (securityManager != null) {
                groups = securityManager.getAuthenticatedUserGroups();
            }
            if(groups!=null && groups.size()>0) {
                //belongs to candidate group and task is not assigned
                restriction = restriction.or(task.taskCandidateGroups.any().groupId.in(groups)
                                                     .and(isNotAssigned));
            }

            //or there are no candidates set and task is not assigned
            restriction = restriction.or(task.taskCandidateUsers.isEmpty()
                                                 .and(task.taskCandidateGroups.isEmpty())
                                                 .and(isNotAssigned));

        }

        return addAndConditionToPredicate(predicate,restriction);
    }

    private Predicate addAndConditionToPredicate(Predicate predicate, BooleanExpression expression){
        if(expression != null && predicate !=null){
            return expression.and(predicate);
        }
        if(expression == null){
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
}
