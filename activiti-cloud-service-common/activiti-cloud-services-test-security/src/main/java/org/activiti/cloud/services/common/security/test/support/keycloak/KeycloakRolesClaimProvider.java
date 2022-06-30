package org.activiti.cloud.services.common.security.test.support.keycloak;

import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.common.security.test.support.RolesClaimProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "activiti.cloud.services.oauth2.iam-name", havingValue = "keycloak", matchIfMissing = true)
public class KeycloakRolesClaimProvider implements RolesClaimProvider {

    @Override
    public void setResourceRoles(Map<String, String[]> resourceRoles, Map<String, Object> claims) {
        JSONObject resourceAccess = new JSONObject();
        for (String key : resourceRoles.keySet()) {
            JSONObject resourceRolesJSON = new JSONObject();
            JSONArray resourceRolesArray = new JSONArray();
            resourceRolesArray.addAll(Arrays.asList(resourceRoles.get(key)));
            resourceRolesJSON.put("roles", resourceRolesArray);
            resourceAccess.put(key, resourceRolesJSON);
        }
        claims.put("resource_access", resourceAccess);
    }

    @Override
    public void setGlobalRoles(Set<String> globalRoles, Map<String, Object> claims) {
        JSONObject realmAccess = new JSONObject();
        JSONArray globalRolesArray = new JSONArray();
        globalRolesArray.addAll(globalRoles);
        realmAccess.put("roles", globalRolesArray);
        claims.put("realm_access", realmAccess);
    }

}
