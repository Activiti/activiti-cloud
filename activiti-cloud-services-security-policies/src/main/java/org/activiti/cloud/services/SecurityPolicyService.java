package org.activiti.cloud.services;

import org.activiti.conf.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class SecurityPolicyService {

    private SecurityProperties securityProperties;

    @Autowired
    public SecurityPolicyService(SecurityProperties securityProperties){
        this.securityProperties = securityProperties;
    }

    public boolean policiesDefined(){
        return ((securityProperties.getGroup()!=null && !securityProperties.getGroup().isEmpty()) || (securityProperties.getUser()!=null && !securityProperties.getUser().isEmpty()));
    }


    public Set<String> getProcessDefinitionKeys(String userId, Collection<String> groups, Collection<SecurityPolicy> policyLevels){


        Set<String> procDefKeys = new HashSet<String>();

        if(groups != null) {
            for (String group : groups) {
                getProcDefKeysForUserOrGroup(policyLevels, procDefKeys, group, securityProperties.getGroup());
            }
        }

        getProcDefKeysForUserOrGroup(policyLevels, procDefKeys, userId, securityProperties.getUser());

        return procDefKeys;

    }

    private void getProcDefKeysForUserOrGroup(Collection<SecurityPolicy> policyLevels, Set<String> procDefKeys, String userOrGroup, Map<String, String> policies) {

        if(userOrGroup == null || policies == null){
            return;
        }

        // iterate through the properties either by user or group (already pre-filtered)

        for(String key: policies.keySet()){

            if(keyMatchesUserOrGroup(userOrGroup, key)){

                for(SecurityPolicy policyLevel:policyLevels){

                    if(keyMatchesPolicyLevel(key, policyLevel)){

                        procDefKeys.addAll(getProcDefKeysFromPropertyValue(policies.get(key)));
                    }
                }
            }
        }
    }

    private boolean keyMatchesPolicyLevel(String key, SecurityPolicy policyLevel) {
        return policyLevel !=null && key.toLowerCase().contains("."+policyLevel.name().toLowerCase()); //note . at beginning
    }

    private boolean keyMatchesUserOrGroup(String userOrGroup, String key) {
        return key!=null && key.toLowerCase().contains(userOrGroup.toLowerCase()+"."); //note . at end
    }

    private Collection<String> getProcDefKeysFromPropertyValue(String propertyValue) {

        if(propertyValue!=null && propertyValue.contains(",")){

            return Arrays.asList(propertyValue.split(","));

        } else if(propertyValue!=null && !propertyValue.isEmpty()){

            return Arrays.asList(propertyValue);
        }

        return new ArrayList<>();
    }

    public Set<String> getProcessDefinitionKeys(String userId, Collection<String> groups, SecurityPolicy minPolicyLevel){
        if (minPolicyLevel != null && minPolicyLevel.equals(SecurityPolicy.READ)){
            return getProcessDefinitionKeys(userId,groups,Arrays.asList(SecurityPolicy.READ,SecurityPolicy.WRITE));
        }
        return getProcessDefinitionKeys(userId,groups,Arrays.asList(minPolicyLevel));
    }

}