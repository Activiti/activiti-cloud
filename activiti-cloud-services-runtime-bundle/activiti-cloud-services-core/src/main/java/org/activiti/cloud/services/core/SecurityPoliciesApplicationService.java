package org.activiti.cloud.services.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.security.BaseSecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPoliciesService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.runtime.api.model.payloads.GetProcessDefinitionsPayload;
import org.activiti.runtime.api.model.payloads.GetProcessInstancesPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecurityPoliciesApplicationService extends BaseSecurityPoliciesApplicationService {


    @Autowired
    private SecurityPoliciesService securityPoliciesService;

    @Autowired
    private SecurityPoliciesProcessDefinitionRestrictionApplier processDefinitionRestrictionApplier;

    @Autowired
    private SecurityPoliciesProcessInstanceRestrictionApplier processInstanceRestrictionApplier;

    @Autowired(required = false)
    private RuntimeBundleProperties runtimeBundleProperties;

    public GetProcessDefinitionsPayload restrictProcessDefQuery(SecurityPolicy securityPolicy){
        return restrictQuery(processDefinitionRestrictionApplier, securityPolicy);
    }

    private Set<String> definitionKeysAllowedForRBPolicy(SecurityPolicy securityPolicy) {
        Map<String,Set<String>> restrictions = definitionKeysAllowedForPolicy(securityPolicy);
        Set<String> keys = new HashSet<>();

        for(String appName:restrictions.keySet()) {
            //only take policies for this app
            //or if we don't know our own appName (just being defensive) then include everything
            //ignore hyphens and case due to values getting set via env vars
            if((runtimeBundleProperties==null || runtimeBundleProperties.getServiceName()==null) ||
                    (appName!=null && appName.replace("-","").equalsIgnoreCase(runtimeBundleProperties.getServiceName().replace("-","")))
                    || (runtimeBundleProperties.getServiceFullName()!=null &&appName!=null && appName.replace("-","").equalsIgnoreCase(runtimeBundleProperties.getServiceFullName().replace("-","")))) {
                keys.addAll(restrictions.get(appName));
            }
        }
        return keys;
    }


    public GetProcessInstancesPayload restrictProcessInstQuery(SecurityPolicy securityPolicy){
        return restrictQuery(processInstanceRestrictionApplier, securityPolicy);
    }

    private  <T> T restrictQuery(SecurityPoliciesRestrictionApplier<T> restrictionApplier, SecurityPolicy securityPolicy){
        if (noSecurityPoliciesOrNoUser()){
            return restrictionApplier.allowAll();
        }

        Set<String> keys = definitionKeysAllowedForRBPolicy(securityPolicy);

        if(keys != null && !keys.isEmpty()){

            if(keys.contains(securityPoliciesService.getWildcard())){
                return restrictionApplier.allowAll();
            }

            return restrictionApplier.restrictToKeys(keys);
        }

        //policies are in place but if we've got here then none for this user
        if((keys == null || keys.isEmpty()) && securityPoliciesService.policiesDefined()) {
            return restrictionApplier.denyAll();
        }

        return restrictionApplier.allowAll();
    }

    public boolean canWrite(String processDefinitionKey){
        return hasPermission(processDefinitionKey, SecurityPolicy.WRITE, runtimeBundleProperties.getServiceName())
                || hasPermission(processDefinitionKey, SecurityPolicy.WRITE, runtimeBundleProperties.getServiceFullName());
    }

    public boolean canRead(String processDefinitionKey){
        return hasPermission(processDefinitionKey, SecurityPolicy.READ,runtimeBundleProperties.getServiceName())
                || hasPermission(processDefinitionKey, SecurityPolicy.WRITE,runtimeBundleProperties.getServiceFullName());
    }

    protected boolean anEntryInSetStartsKey(Set<String> keys, String processDefinitionKey){
        for(String key:keys){
            //override the base class with exact matching as startsWith is only preferable for audit where id might be used that would start with key
            if(processDefinitionKey.equalsIgnoreCase(key)){
                return true;
            }
        }
        return false;
    }

}
