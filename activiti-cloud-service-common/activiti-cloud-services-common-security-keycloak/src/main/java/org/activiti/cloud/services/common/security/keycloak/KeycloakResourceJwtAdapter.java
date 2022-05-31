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
package org.activiti.cloud.services.common.security.keycloak;

import com.nimbusds.jose.shaded.json.JSONObject;
import java.util.Collections;
import java.util.List;
import org.activiti.cloud.services.common.security.jwt.JwtAdapter;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakResourceJwtAdapter implements JwtAdapter {

    private final String resourceId;
    private final Jwt jwt;

    public KeycloakResourceJwtAdapter(String resourceId, Jwt jwt) {
        this.resourceId = resourceId;
        this.jwt = jwt;
    }

    @Override
    public Jwt getJwt() {
        return jwt;
    }

    @Override
    public List<String> getRoles() {
        return getFromClient(resourceId, jwt);
    }

    @Override
    public List<String> getGroups() {
        List<String> groups = jwt.getClaimAsStringList("groups");
        return groups.isEmpty() ? null : groups;
    }

    @Override
    public String getUserName() {
        return jwt.getClaim("preferred_username");
    }

    private List<String> getFromClient(String clientId, Jwt jwt) {
        JSONObject resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess.get(clientId) != null) {
            return getRoles((JSONObject) resourceAccess.get(clientId));
        } else {
            return Collections.emptyList();
        }
    }

    private List<String> getRoles(JSONObject getRolesParent) {
        return (List<String>) getRolesParent.get("roles");
    }
}
