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
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.identity.model.UserApplicationAccess;
import org.activiti.cloud.identity.model.UserRoles;
import org.activiti.cloud.identity.model.UserRoles.UserGlobalAccess;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Access;

public class KeycloakTokenToUserRoles {

    public UserRoles toUserRoles(AccessToken token) {
        UserRoles userRoles = new UserRoles();
        if(token != null) {
            List<UserApplicationAccess> applicationAccess = getAppAccessFromResourceAccess(token.getResourceAccess());
            userRoles.setApplicationAccess(applicationAccess);

            Set<String> globalRoles = getRolesFromAccess(token.getRealmAccess());
            userRoles.setGlobalAccess(new UserGlobalAccess(globalRoles));
        }
        return userRoles;
    }

    private List<UserApplicationAccess> getAppAccessFromResourceAccess(Map<String, Access> resourceAccess) {
        return Optional.ofNullable(resourceAccess)
            .map(Map::entrySet)
            .orElse(Collections.emptySet())
            .stream()
            .map(e -> new UserApplicationAccess(e.getKey(), getRolesFromAccess(e.getValue())))
            .collect(Collectors.toList());
    }

    private Set<String> getRolesFromAccess(Access access) {
        return Optional.ofNullable(access)
            .map(Access::getRoles)
            .orElse(Collections.emptySet());
    }
}
