package org.activiti.cloud.services.core;

import org.activiti.cloud.services.SecurityPolicy;
import org.activiti.cloud.services.SecurityPoliciesService;
import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
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


    public ProcessDefinitionQuery restrictProcessDefQuery(ProcessDefinitionQuery query, SecurityPolicy securityPolicy){

        if (noSecurityPoliciesOrNoUser()){
            return query;
        }

        Set<String> keys = definitionKeysAllowedForPolicy(securityPolicy);

        if(keys != null){ //restrict query to only these keys
            return query.processDefinitionKeys(keys);
        }
        return query;
    }

    private boolean noSecurityPoliciesOrNoUser() {
        return !securityPoliciesService.policiesDefined() || authenticationWrapper.getAuthenticatedUserId()== null;
    }

    private Set<String> definitionKeysAllowedForPolicy(SecurityPolicy securityPolicy) {
        List<String> groups = null;

        if(userGroupLookupProxy!=null && authenticationWrapper.getAuthenticatedUserId()!=null){
            groups = userGroupLookupProxy.getGroupsForCandidateUser(authenticationWrapper.getAuthenticatedUserId());
        }

        return securityPoliciesService.getProcessDefinitionKeys(authenticationWrapper.getAuthenticatedUserId(),
                groups, securityPolicy);
    }

    public ProcessInstanceQuery restrictProcessInstQuery(ProcessInstanceQuery query, SecurityPolicy securityPolicy){
        if (noSecurityPoliciesOrNoUser()){
            return query;
        }

        Set<String> keys = definitionKeysAllowedForPolicy(securityPolicy);

        if(keys != null){
            return query.processDefinitionKeys(keys);
        }
        return query;
    }

    public boolean canWrite(String processDefId){
        return hasPermission(processDefId, SecurityPolicy.WRITE);
    }

    public boolean canRead(String processDefId){
        return hasPermission(processDefId, SecurityPolicy.READ);
    }

    private boolean hasPermission(String processDefId, SecurityPolicy securityPolicy){

        if (!securityPoliciesService.policiesDefined() || userGroupLookupProxy == null || authenticationWrapper.getAuthenticatedUserId() == null){
            return true;
        }

        if(userRoleLookupProxy != null && userRoleLookupProxy.isAdmin(authenticationWrapper.getAuthenticatedUserId())){
            return true;
        }

        Set<String> keys = definitionKeysAllowedForPolicy(securityPolicy);

        return (keys != null && keys.contains(processDefId));
    }

}
