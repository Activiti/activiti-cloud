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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakUser;

public class KeycloakUserGroupManager implements UserGroupManager {


    private final KeycloakClient keycloakClient;

    public KeycloakUserGroupManager(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @Override
    public List<String> getUserGroups(String username) {
        KeycloakUser user = loadRepresentation(username);

        List<KeycloakGroup> groupRepresentations = keycloakClient.getUserGroups(user.getId());

        List<String> groups = null;
        if (groupRepresentations != null && groupRepresentations.size() > 0) {
            groups = new ArrayList<>();
            for (KeycloakGroup groupRepresentation : groupRepresentations) {
                groups.add(groupRepresentation.getName());
            }
        }

        return groups;
    }

    @Override
    public List<String> getUserRoles(String username) {
        KeycloakUser user = loadRepresentation(username);

        List<KeycloakRoleMapping> rolesRepresentations = keycloakClient.getUserRoleMapping(user.getId());

        List<String> roles = null;
        if (rolesRepresentations != null && rolesRepresentations.size() > 0) {
            roles = new ArrayList<>();
            for (KeycloakRoleMapping roleRepresentation : rolesRepresentations) {
                roles.add(roleRepresentation.getName());
            }
        }

        return roles;
    }

    @Override
    public List<String> getGroups() {
        return keycloakClient.getAllGroups()
                .stream().map(KeycloakGroup::getName).collect(Collectors.toList());
    }

    @Override
    public List<String> getUsers() {
        return keycloakClient.getAllUsers(keycloakClient.countAllUsers())
                .stream().map(KeycloakUser::getUsername).collect(Collectors.toList());
    }

    private KeycloakUser loadRepresentation(String username) {
        List<KeycloakUser> users = keycloakClient.searchUsers(username, 0, 2);

        if (users.size() > 1) {
            throw new UnsupportedOperationException("User id " + username + " is not unique");
        }
        if (users.size() == 0) {
            throw new UnsupportedOperationException("User id " + username + " not found");
        }
        return users.get(0);
    }
}
