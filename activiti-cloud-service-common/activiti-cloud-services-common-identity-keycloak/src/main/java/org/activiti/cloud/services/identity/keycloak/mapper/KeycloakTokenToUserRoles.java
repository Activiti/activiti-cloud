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
package org.activiti.cloud.services.identity.keycloak.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minidev.json.JSONObject;
import org.activiti.cloud.identity.model.UserApplicationAccess;
import org.activiti.cloud.identity.model.UserRoles;
import org.activiti.cloud.identity.model.UserRoles.UserGlobalAccess;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakTokenToUserRoles {

    public static final String ROLES = "roles";
    public static final String RESOURCE = "resource_access";
    public static final String REALM = "realm_access";

    public static UserRoles toUserRoles(Jwt token) {
        UserRoles userRoles = new UserRoles();

        if (token != null) {
            List<UserApplicationAccess> applicationAccess = getResourceRoles(token);
            userRoles.setApplicationAccess(applicationAccess);

            List<String> globalRoles = getGlobalRoles(token);
            userRoles.setGlobalAccess(new UserGlobalAccess(globalRoles));
        }
        return userRoles;
    }

    private static List<UserApplicationAccess> getResourceRoles(Jwt token) {
        return Optional
            .ofNullable(token.getClaimAsMap(RESOURCE))
            .map(Map::entrySet)
            .orElse(Collections.emptySet())
            .stream()
            .map(e -> new UserApplicationAccess(e.getKey(), getRoles((Map<String, Object>) e.getValue())))
            .collect(Collectors.toList());
    }

    private static List<String> getGlobalRoles(Jwt jwt) {
        if (jwt.getClaim(REALM) != null) {
            return getRoles(jwt.getClaim(REALM));
        } else {
            return Collections.emptyList();
        }
    }

    private static List<String> getRoles(Map<String, Object> getRolesParent) {
        return Optional.ofNullable((List<String>) getRolesParent.get(ROLES)).orElse(Collections.emptyList());
    }
}
