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
