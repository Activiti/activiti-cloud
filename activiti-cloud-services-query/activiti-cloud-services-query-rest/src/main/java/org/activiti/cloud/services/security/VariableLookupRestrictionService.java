package org.activiti.cloud.services.security;

import com.querydsl.core.types.Predicate;
import org.activiti.spring.security.policies.SecurityPolicyAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is present in case of a future scenario where we need to filter task or process instance variables more generally rather than per task or per proc.
 */
@Component
public class VariableLookupRestrictionService {

    @Autowired
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @Autowired
    private SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService;


    public Predicate restrictTaskVariableQuery(Predicate predicate){
        return taskLookupRestrictionService.restrictTaskVariableQuery(predicate);
    }

    public Predicate restrictProcessInstanceVariableQuery(Predicate predicate){
        return securityPoliciesApplicationService.restrictProcessInstanceVariableQuery(predicate, SecurityPolicyAccess.READ);
    }

}
