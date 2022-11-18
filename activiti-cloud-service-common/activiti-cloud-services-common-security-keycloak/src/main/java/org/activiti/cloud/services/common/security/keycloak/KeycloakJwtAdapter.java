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

public class KeycloakJwtAdapter implements JwtAdapter {

    private final Jwt jwt;

    public KeycloakJwtAdapter(Jwt jwt) {
        this.jwt = jwt;
    }

    public Jwt getJwt() {
        return jwt;
    }

    @Override
    public List<String> getRoles() {
        if (jwt.hasClaim("realm_access")) {
            return getRoles(jwt.getClaim("realm_access"));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getUserName() {
        return jwt.getClaim("preferred_username");
    }

    @Override
    public List<String> getGroups() {
        if (jwt.hasClaim("groups")) {
            return jwt.getClaimAsStringList("groups");
        } else {
            return null;
        }
    }

    private List<String> getRoles(JSONObject getRolesParent) {
        return (List<String>) getRolesParent.get("roles");
    }
}
