package org.activiti.cloud.services.audit.jpa.security;

import java.util.Map;
import java.util.Set;

import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.spring.security.policies.BaseSecurityPoliciesManagerImpl;
import org.activiti.spring.security.policies.SecurityPoliciesManager;
import org.activiti.spring.security.policies.SecurityPolicyAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Applies security policies (defined into the application.properties file) to event data
 */
@Component
public class SecurityPoliciesApplicationServiceImpl extends BaseSecurityPoliciesManagerImpl implements SecurityPoliciesManager {


    /*
     * Apply Filters for Security Policies (configured in application.properties
     * Steps
     *  - If no Security Policies or no User, return unmodified
     *  - For Each Security Policy
     *    - Get Process Definitions associated with the policy
     *    - If it was not a wildcard
     *      - Add Policy for Service Name, ProcessDefinition pair
     *    - If it was a wildcard
     *      - Add Policy for Service Name only
     *  - If no other policies applied
     *    - Add Impossible filter so the user doesn't get any data
     */
    public Specification<AuditEventEntity> createSpecWithSecurity(Specification<AuditEventEntity> spec,
                                                                  SecurityPolicyAccess securityPolicy) {
        if(spec == null){
            spec = new AlwaysTrueSpecification();
        }
        if (!arePoliciesDefined()) {
            return spec;
        }
        Map<String, Set<String>> restrictions = getAllowedKeys(securityPolicy);

        for (String serviceName : restrictions.keySet()) {

            Set<String> defKeys = restrictions.get(serviceName);
            //will filter by app name and will also filter by definition keys if no wildcard,
            if (defKeys != null && defKeys.size()>0 && !defKeys.contains(securityPoliciesProperties.getWildcard())) {
                return spec.and(new ApplicationProcessDefSecuritySpecification(serviceName,
                                                                               defKeys));
            } else if (defKeys != null && defKeys.contains(securityPoliciesProperties.getWildcard())) {  //will filter by app name if wildcard is set
                return spec.and(new ApplicationSecuritySpecification(serviceName));
            }
        }
        //policies are defined but none are applicable
        if (arePoliciesDefined()) {
            //user should not see anything so give unsatisfiable condition
            return spec.and(new ImpossibleSpecification());
        }

        return spec;
    }

    public boolean canWrite(String processDefinitionKey) {
        //TODO: change interface?
        System.err.println("Unusused method - does it have to be in interface?");
        return false;
    }

    public boolean canRead(String processDefinitionKey) {
        //TODO: change interface?
        System.err.println("Unusused method - does it have to be in interface?");
        return false;
    }
}
