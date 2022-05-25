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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.identity.GroupSearchParams;
import org.activiti.cloud.identity.IdentityManagementService;
import org.activiti.cloud.identity.UserSearchParams;
import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.Role;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.identity.model.UserRoles;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakGroupToGroup;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakRoleMappingToRole;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakTokenToUserRoles;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakUserToUser;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakClientRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class KeycloakManagementService implements IdentityManagementService {

    public static final int PAGE_START = 0;
    public static final int PAGE_SIZE = 50;

    private final KeycloakClient keycloakClient;
    private final KeycloakUserToUser keycloakUserToUser;
    private final KeycloakGroupToGroup keycloakGroupToGroup;
    private final KeycloakTokenToUserRoles keycloakTokenToUserRoles;
    private final KeycloakRoleMappingToRole keycloakRoleMappingToRole;

    public KeycloakManagementService(KeycloakClient keycloakClient,
        KeycloakUserToUser keycloakUserToUser,
        KeycloakGroupToGroup keycloakGroupToGroup,
        KeycloakTokenToUserRoles keycloakTokenToUserRoles,
        KeycloakRoleMappingToRole keycloakRoleMappingToRole) {
        this.keycloakClient = keycloakClient;
        this.keycloakUserToUser = keycloakUserToUser;
        this.keycloakGroupToGroup = keycloakGroupToGroup;
        this.keycloakTokenToUserRoles = keycloakTokenToUserRoles;
        this.keycloakRoleMappingToRole = keycloakRoleMappingToRole;
    }

    @Override
    public List<User> findUsers(UserSearchParams userSearchParams) {
        List<User> users= keycloakClient
            .searchUsers(userSearchParams.getSearchKey(), PAGE_START, PAGE_SIZE)
            .stream()
            .map(keycloakUserToUser::toUser)
            .collect(Collectors.toList());

        if(!StringUtils.isEmpty(userSearchParams.getApplication())) {
            return filterUsersInApplicationsScope(users, userSearchParams);
        } else {
            return filterUsers(users, userSearchParams);
        }
    }

    private List<User> filterUsers(List<User> users ,UserSearchParams userSearchParams) {
        return users
            .stream()
            .filter(user -> filterByRoles(user.getRoles(), userSearchParams.getRoles()))
            .filter(user -> filterByGroups(user, userSearchParams.getGroups()))
            .collect(Collectors.toList());
    }

    private List<User> filterUsersInApplicationsScope(List<User> users ,UserSearchParams userSearchParams) {
        String application = userSearchParams.getApplication();
        String kClientId = getKeycloakClientId(application);
        if(StringUtils.isEmpty(kClientId)) {
            return Collections.emptyList();
        }
       users.forEach(user -> user.setApplicationRoles(
           Map.of(application,
               getUserApplicationRoles(user.getId(), kClientId))));

        return users
            .stream()
            .filter(user -> filterByApplication(user, application))
            .filter(user -> filterByRoles(user.getApplicationRoles().get(application), userSearchParams.getRoles()))
            .filter(user -> filterByGroups(user, userSearchParams.getGroups()))
            .collect(Collectors.toList());
    }

    private boolean filterByGroups(User user, Set<String> groups) {
        return CollectionUtils.isEmpty(groups) || keycloakClient
            .getUserGroups(user.getId())
            .stream()
            .map(KeycloakGroup::getName)
            .anyMatch(groups::contains);
    }

    @Override
    public List<Group> findGroups(GroupSearchParams groupSearchParams) {
        return keycloakClient
            .searchGroups(groupSearchParams.getSearch(), PAGE_START, PAGE_SIZE)
            .stream()
            .map(keycloakGroupToGroup::toGroup)
            .filter(group -> filterByRoles(group.getRoles(), groupSearchParams.getRoles()))
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

    @Override
    public UserRoles getUserRoles(Jwt principal) {
        return keycloakTokenToUserRoles.toUserRoles(principal);
    }

    private boolean filterByApplication(User user, String application) {
        return user
            .getApplicationRoles()
            .get(application)
            .stream()
            .findAny()
            .isPresent();
    }

    private List<Role> getUserApplicationRoles(String userId, String clientId) {
        if (!clientId.isEmpty()) {
            return keycloakClient.getUserClientRoleMapping(userId, clientId)
                .stream()
                .map(keycloakRoleMappingToRole::toRole)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String getKeycloakClientId(String application) {
        List<KeycloakClientRepresentation> kClients = keycloakClient.searchClients(application, 0, 1);
        return !kClients.isEmpty() ? kClients.get(0).getId() : null;
    }
}
