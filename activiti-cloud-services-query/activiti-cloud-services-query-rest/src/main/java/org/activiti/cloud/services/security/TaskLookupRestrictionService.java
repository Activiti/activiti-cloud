package org.activiti.cloud.services.security;

import java.util.List;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.activiti.cloud.services.query.model.QTask;
import org.activiti.cloud.services.query.model.QVariable;
import org.activiti.engine.UserGroupLookupProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
 * Tested by RestrictTaskQueryIT
 * Applies permissions/restrictions to Task data (and Task Variables) based upon Candidate user/group logic
 */
@Component
public class TaskLookupRestrictionService {

    @Autowired(required = false)
    private UserGroupLookupProxy userGroupLookupProxy;

    @Autowired
    private AuthenticationWrapper authenticationWrapper;

    @Value("${activiti.cloud.security.task.restrictions.enabled:true}")
    private boolean restrictionsEnabled;

    public Predicate restrictTaskQuery(Predicate predicate){

        return restrictTaskQuery(predicate,QTask.task);
    }


    public Predicate restrictTaskVariableQuery(Predicate predicate){

        QTask task = QVariable.variable.task;

        Predicate extendedPredicate = addAndConditionToPredicate(predicate,task.isNotNull());

        return restrictTaskQuery(extendedPredicate, task);
    }

    private Predicate restrictTaskQuery(Predicate predicate, QTask task){

        if (!restrictionsEnabled){
            return predicate;
        }

        //get authenticated user
        String userId = authenticationWrapper.getAuthenticatedUserId();

        BooleanExpression restriction = null;

        if(userId!=null) {

            //user is assignee
            restriction = addOrConditionToExpression(restriction,task.assignee.eq(userId));

            //or user is a candidate
            restriction = addOrConditionToExpression(restriction,task.taskCandidateUsers.any().userId.eq(userId));

            //or one of user's group is candidate

            List<String> groups = null;
            if (userGroupLookupProxy != null) {
                groups = userGroupLookupProxy.getGroupsForCandidateUser(userId);
            }
            if(groups!=null && groups.size()>0) {
                restriction = addOrConditionToExpression(restriction,task.taskCandidateGroups.any().groupId.in(groups));
            }

            //or there are no candidates set
            restriction = addOrConditionToExpression(restriction,task.taskCandidateUsers.isEmpty().and(task.taskCandidateGroups.isEmpty()));

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

    private BooleanExpression addOrConditionToExpression(BooleanExpression predicate, BooleanExpression expression){
        if(expression != null && predicate !=null){
            return expression.or(predicate);
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
