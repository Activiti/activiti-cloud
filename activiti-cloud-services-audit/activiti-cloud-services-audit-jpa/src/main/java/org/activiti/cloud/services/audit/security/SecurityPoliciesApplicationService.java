package org.activiti.cloud.services.audit.security;

import java.util.Map;
import java.util.Set;

import org.activiti.cloud.services.audit.events.AuditEventEntity;
import org.activiti.cloud.services.security.BaseSecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPoliciesService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Applies security policies (defined into the application.properties file) to event data
 */
@Component
public class SecurityPoliciesApplicationService extends BaseSecurityPoliciesApplicationService {

    @Autowired
    private SecurityPoliciesService securityPoliciesService;

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
                                                                  SecurityPolicy securityPolicy) {
        if (noSecurityPoliciesOrNoUser()) {
            return spec;
        }
        Map<String, Set<String>> restrictions = definitionKeysAllowedForPolicy(securityPolicy);

        for (String serviceName : restrictions.keySet()) {

            Set<String> defKeys = restrictions.get(serviceName);
            //will filter by app name and will also filter by definition keys if no wildcard
            if (!defKeys.contains(securityPoliciesService.getWildcard())) {
                return spec.and(new ApplicationProcessDefSecuritySpecification(serviceName,
                                                                               defKeys));
            } else {  //will filter by app name if wildcard is set
                return spec.and(new ApplicationSecuritySpecification(serviceName));
            }
        }
        //policies are defined but none are applicable
        if (securityPoliciesService.policiesDefined()) {
            //user should not see anything so give unsatisfiable condition
            return spec.and(new ImpossibleSpecification());
        }

        return spec;
    }
}
