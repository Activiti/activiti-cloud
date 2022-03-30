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
package org.activiti.cloud.services.identity.keycloak;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.identity.IdentityManagementService;
import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.Role;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakGroupToGroup;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakUserToUser;
import org.springframework.util.CollectionUtils;

public class KeycloakManagementService implements IdentityManagementService {

    private final KeycloakClient keycloakClient;
    private final KeycloakUserToUser keycloakUserToUser;
    private final KeycloakGroupToGroup keycloakGroupToGroup;

    public KeycloakManagementService(KeycloakClient keycloakClient,
                                     KeycloakUserToUser keycloakUserToUser,
                                     KeycloakGroupToGroup keycloakGroupToGroup) {
        this.keycloakClient = keycloakClient;
        this.keycloakUserToUser = keycloakUserToUser;
        this.keycloakGroupToGroup = keycloakGroupToGroup;
    }

    @Override
    public List<User> findUsers(String key, Set<String> roles, Integer page, Integer size) {
        return keycloakClient
            .searchUsers(key, calculateFirst(page, size), size)
            .stream()
            .map(keycloakUserToUser::toUser)
            .filter(user -> filterByRoles(user.getRoles(), roles))
            .collect(Collectors.toList());
    }

    @Override
    public List<Group> findGroups(String key, Set<String> roles, Integer page, Integer size) {
        return keycloakClient
            .searchGroups(key, calculateFirst(page, size), size)
            .stream()
            .map(keycloakGroupToGroup::toGroup)
            .filter(user -> filterByRoles(user.getRoles(), roles))
            .collect(Collectors.toList());
    }

    private boolean filterByRoles(List<Role> currentRoles, Set<String> filterRoles) {
        return CollectionUtils.isEmpty(filterRoles) ||
            currentRoles != null && currentRoles
                .stream()
                .filter(role -> filterRoles.contains(role.getName()))
                .findAny()
                .isPresent();
    }

    private int calculateFirst(Integer page, Integer size) {
        return page * size;
    }


}
