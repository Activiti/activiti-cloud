package org.activiti.cloud.services.security;

import com.querydsl.core.types.Predicate;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;

/**
 * This is present in case of a future scenario where we need to filter task or process instance variables more generally rather than per task or per proc.
 */
public class ProcessVariableLookupRestrictionService {

    private final ProcessVariableRestrictionService restrictionService;

    public ProcessVariableLookupRestrictionService(ProcessVariableRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    public Predicate restrictProcessInstanceVariableQuery(Predicate predicate){
        return restrictionService.restrictProcessInstanceVariableQuery(predicate, SecurityPolicyAccess.READ);
    }

}
