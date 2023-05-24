/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.common.security.test.support.keycloak;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.activiti.cloud.services.common.security.test.support.RolesClaimProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@AutoConfiguration
@Component
@ConditionalOnProperty(
    value = "activiti.cloud.services.oauth2.iam-name",
    havingValue = "keycloak",
    matchIfMissing = true
)
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
