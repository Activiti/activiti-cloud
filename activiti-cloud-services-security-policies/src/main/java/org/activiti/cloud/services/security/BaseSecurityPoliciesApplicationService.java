package org.activiti.cloud.services.security;

import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseSecurityPoliciesApplicationService {

    @Autowired(required = false)
    protected UserGroupLookupProxy userGroupLookupProxy;

    @Autowired(required = false)
    protected UserRoleLookupProxy userRoleLookupProxy;

    @Autowired
    protected BaseAuthenticationWrapper authenticationWrapper;

    @Autowired
    protected SecurityPoliciesService securityPoliciesService;


    protected boolean noSecurityPoliciesOrNoUser() {
        return !securityPoliciesService.policiesDefined() || authenticationWrapper.getAuthenticatedUserId() == null;
    }

    protected Map<String, Set<String>> definitionKeysAllowedForPolicy(SecurityPolicy securityPolicy) {
        List<String> groups = null;

        if (userGroupLookupProxy != null && authenticationWrapper.getAuthenticatedUserId() != null) {
            groups = userGroupLookupProxy.getGroupsForCandidateUser(authenticationWrapper.getAuthenticatedUserId());
        }

        return securityPoliciesService.getProcessDefinitionKeys(authenticationWrapper.getAuthenticatedUserId(),
                groups,
                securityPolicy);
    }

    public boolean canRead(String processDefId,
                           String appName) {
        return hasPermission(processDefId,
                SecurityPolicy.READ,
                appName);
    }


    public boolean canWrite(String processDefId, String appName){
        return hasPermission(processDefId, SecurityPolicy.WRITE,appName);
    }


    protected boolean hasPermission(String processDefId,
                                  SecurityPolicy securityPolicy,
                                  String appName) {

        if (!securityPoliciesService.policiesDefined() || userGroupLookupProxy == null || authenticationWrapper.getAuthenticatedUserId() == null) {
            return true;
        }

        if (userRoleLookupProxy != null && userRoleLookupProxy.isAdmin(authenticationWrapper.getAuthenticatedUserId())) {
            return true;
        }

        Set<String> keys = new HashSet();
        Map<String, Set<String>> policiesMap = definitionKeysAllowedForPolicy(securityPolicy);
        if(policiesMap.get(appName) !=null) {
            keys.addAll(policiesMap.get(appName));
        }
        //also factor for case sensitivity and hyphens (which are stripped when specified through env var)
        if(policiesMap.get(appName.replaceAll("-","").toLowerCase()) != null){
            keys.addAll(policiesMap.get(appName.replaceAll("-","").toLowerCase()));
        }

        return (keys != null && (anEntryInSetStartsId(keys,processDefId) || keys.contains(securityPoliciesService.getWildcard())));
    }

    protected boolean anEntryInSetStartsId(Set<String> keys,String processDefId){
        for(String key:keys){
            if(processDefId.startsWith(key)){
                return true;
            }
        }
        return false;
    }
}
