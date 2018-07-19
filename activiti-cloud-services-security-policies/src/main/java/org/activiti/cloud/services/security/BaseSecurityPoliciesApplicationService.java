package org.activiti.cloud.services.security;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.runtime.api.auth.AuthorizationLookup;
import org.activiti.runtime.api.identity.IdentityLookup;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseSecurityPoliciesApplicationService implements SecurityPoliciesApplicationService {

    @Autowired
    protected IdentityLookup identityLookup;

    @Autowired
    protected AuthorizationLookup authorizationLookup;

    @Autowired
    protected SpringSecurityAuthenticationWrapper authenticationWrapper;

    @Autowired
    protected SecurityPoliciesService securityPoliciesService;

    protected boolean noSecurityPoliciesOrNoUser() {
        return !securityPoliciesService.policiesDefined() || authenticationWrapper.getAuthenticatedUserId() == null;
    }

    protected Map<String, Set<String>> definitionKeysAllowedForPolicy(SecurityPolicy securityPolicy) {
        List<String> groups = null;

        if (identityLookup != null && authenticationWrapper.getAuthenticatedUserId()!= null) {
            groups = identityLookup.getGroupsForCandidateUser(authenticationWrapper.getAuthenticatedUserId());
        }

        return securityPoliciesService.getProcessDefinitionKeys(authenticationWrapper.getAuthenticatedUserId(),
                groups,
                securityPolicy);
    }

    @Override
    public boolean canRead(String processDefinitionKey,
                           String appName) {
        return hasPermission(processDefinitionKey,
                SecurityPolicy.READ,
                appName);
    }


    @Override
    public boolean canWrite(String processDefinitionKey,
                            String appName){
        return hasPermission(processDefinitionKey, SecurityPolicy.WRITE,appName);
    }


    protected boolean hasPermission(String processDefinitionKey,
                                  SecurityPolicy securityPolicy,
                                  String appName) {

        if (!securityPoliciesService.policiesDefined() || identityLookup == null || authenticationWrapper.getAuthenticatedUserId() == null) {
            return true;
        }

        if (authorizationLookup != null && authorizationLookup.isAdmin(authenticationWrapper.getAuthenticatedUserId())) {
            return true;
        }

        Set<String> keys = new HashSet<>();
        Map<String, Set<String>> policiesMap = definitionKeysAllowedForPolicy(securityPolicy);
        if(policiesMap.get(appName) !=null) {
            keys.addAll(policiesMap.get(appName));
        }
        //also factor for case sensitivity and hyphens (which are stripped when specified through env var)
        if(appName!=null && policiesMap.get(appName.replaceAll("-","").toLowerCase()) != null){
            keys.addAll(policiesMap.get(appName.replaceAll("-","").toLowerCase()));
        }

        return anEntryInSetStartsKey(keys,
                                     processDefinitionKey) || keys.contains(securityPoliciesService.getWildcard());
    }

    //startsWith logic supports the case of audit where only definition id might be available and it would start with the key
    //protected scope means we can override where exact matching more appropriate (consider keys ProcessWithVariables and ProcessWithVariables2)
    //even for audit would be better if we had a known separator which cant be part of key - this seems best we can do for now
    protected boolean anEntryInSetStartsKey(Set<String> keys, String processDefinitionKey){
        for(String key:keys){
            if(processDefinitionKey.startsWith(key)){
                return true;
            }
        }
        return false;
    }
}
