package org.activiti.cloud.services.query.security;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.activiti.cloud.services.query.model.QProcessInstance;
import org.activiti.cloud.services.query.model.QTask;
import org.activiti.cloud.services.query.model.QVariable;
import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.activiti.cloud.services.SecurityPoliciesService;
import org.activiti.cloud.services.SecurityPolicy;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SecurityPoliciesApplicationService {

    @Autowired(required = false)
    private UserGroupLookupProxy userGroupLookupProxy;

    @Autowired(required = false)
    private UserRoleLookupProxy userRoleLookupProxy;

    @Autowired
    private AuthenticationWrapper authenticationWrapper;

    @Autowired
    private SecurityPoliciesService securityPoliciesService;


    public Predicate restrictProcessInstanceQuery(Predicate predicate, SecurityPolicy securityPolicy){
        if (noSecurityPoliciesOrNoUser()){
            return predicate;
        }

        QProcessInstance processInstance = QProcessInstance.processInstance;
        return buildPredicateForQProcessInstance(predicate, securityPolicy, processInstance);

    }

    public Predicate restrictTaskQuery(Predicate predicate, SecurityPolicy securityPolicy){
        if (noSecurityPoliciesOrNoUser()){
            return predicate;
        }

        QProcessInstance processInstance = QTask.task.processInstance;
        return buildPredicateForQProcessInstance(predicate, securityPolicy, processInstance);

    }


    public Predicate restrictVariableQuery(Predicate predicate, SecurityPolicy securityPolicy){
        if (noSecurityPoliciesOrNoUser()){
            return predicate;
        }

        QProcessInstance processInstance = QVariable.variable.processInstance;
        return buildPredicateForQProcessInstance(predicate, securityPolicy, processInstance);

    }


    public Predicate buildPredicateForQProcessInstance(Predicate predicate, SecurityPolicy securityPolicy, QProcessInstance processInstance) {
        BooleanExpression securityExpression = null;

        Map<String,Set<String>> restrictions = definitionKeysAllowedForPolicy(securityPolicy);

        for(String appName:restrictions.keySet()) {
            Set<String> defKeys = restrictions.get(appName);
            securityExpression = addProcessDefRestrictionToExpression(processInstance, securityExpression, appName, defKeys);
        }

        return securityExpression != null ? securityExpression.and(predicate) : predicate;
    }

    public BooleanExpression addProcessDefRestrictionToExpression(QProcessInstance processInstance, BooleanExpression securityExpression, String appName, Set<String> defKeys) {
        BooleanExpression nextExpression = processInstance.processDefinitionKey.in(defKeys).and(processInstance.applicationName.eq(appName));
        if(securityExpression == null){
            securityExpression = nextExpression;
        } else{
            securityExpression = securityExpression.or(nextExpression);
        }
        return securityExpression;
    }


    private boolean noSecurityPoliciesOrNoUser() {
        return !securityPoliciesService.policiesDefined() || authenticationWrapper.getAuthenticatedUserId()== null;
    }


    private Map<String, Set<String>> definitionKeysAllowedForPolicy(SecurityPolicy securityPolicy) {
        List<String> groups = null;

        if(userGroupLookupProxy!=null && authenticationWrapper.getAuthenticatedUserId()!=null){
            groups = userGroupLookupProxy.getGroupsForCandidateUser(authenticationWrapper.getAuthenticatedUserId());
        }

        return securityPoliciesService.getProcessDefinitionKeys(authenticationWrapper.getAuthenticatedUserId(),
                groups, securityPolicy);
    }

    public boolean canWrite(String processDefId, String appName){
        return hasPermission(processDefId, SecurityPolicy.WRITE,appName);
    }

    public boolean canRead(String processDefId, String appName){
        return hasPermission(processDefId, SecurityPolicy.READ,appName);
    }

    private boolean hasPermission(String processDefId, SecurityPolicy securityPolicy, String appName){

        if (!securityPoliciesService.policiesDefined() || userGroupLookupProxy == null || authenticationWrapper.getAuthenticatedUserId() == null){
            return true;
        }

        if(userRoleLookupProxy != null && userRoleLookupProxy.isAdmin(authenticationWrapper.getAuthenticatedUserId())){
            return true;
        }

        Set<String> keys = definitionKeysAllowedForPolicy(securityPolicy).get(appName);

        return (keys != null && keys.contains(processDefId));
    }

}
