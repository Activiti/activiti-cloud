/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakUserGroupManager implements UserGroupManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakUserGroupManager.class);

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

        LOGGER.debug(
            "Resolved group membership for requested username [{}]: resolved user id present [{}], membership count [{}]",
            username,
            user.getId() != null,
            groups != null ? groups.size() : 0
        );

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
        return keycloakClient.getAllGroups().stream().map(KeycloakGroup::getName).collect(Collectors.toList());
    }

    @Override
    public List<String> getUsers() {
        return keycloakClient
            .getAllUsers(keycloakClient.countAllUsers())
            .stream()
            .map(KeycloakUser::getUsername)
            .collect(Collectors.toList());
    }

    private KeycloakUser loadRepresentation(String username) {
        List<KeycloakUser> users = keycloakClient.searchUsers(username, 0, 2);

        if (users.size() > 1) {
            LOGGER.warn(
                "User resolution for requested username [{}] is ambiguous: broad search returned [{}] candidates",
                username,
                users.size()
            );
            throw new UnsupportedOperationException("User id " + username + " is not unique");
        }
        if (users.size() == 0) {
            LOGGER.warn("User resolution for requested username [{}] found no candidates from broad search", username);
            throw new UnsupportedOperationException("User id " + username + " not found");
        }

        KeycloakUser resolved = users.getFirst();
        if (!username.equals(resolved.getUsername())) {
            LOGGER.warn(
                "User resolution for requested username [{}] matched a single user with a different username; " +
                    "broad search is not an exact match",
                username
            );
        }
        return resolved;
    }
}
