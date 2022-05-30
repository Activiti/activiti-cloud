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
import java.util.HashMap;
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
            return filterUsersInRealmScope(users, userSearchParams);
        }
    }

    private List<User> filterUsersInRealmScope(List<User> users ,UserSearchParams userSearchParams) {
        Map<String, List<Role>> usersRolesMapping = new HashMap<>();
        if(!CollectionUtils.isEmpty(userSearchParams.getRoles())) {
            users.forEach(user ->
                usersRolesMapping.put(user.getId(),
                    getUserRealmRoles(user.getId())));
        }
        return users
            .stream()
            .filter(user -> filterByRoles(usersRolesMapping.get(user.getId()), userSearchParams.getRoles()))
            .filter(user -> filterByGroups(user, userSearchParams.getGroups()))
            .collect(Collectors.toList());
    }

    private List<Role> getUserRealmRoles(String userId) {
        return keycloakRoleMappingToRole.toRoles(
                    keycloakClient.getUserRoleMapping(userId));
    }

    private List<User> filterUsersInApplicationsScope(List<User> users ,UserSearchParams userSearchParams) {
        String application = userSearchParams.getApplication();
        String kClientId = getKeycloakClientId(application);
        if(StringUtils.isEmpty(kClientId)) {
            return Collections.emptyList();
        }
       Map<String, List<Role>> userAppRoles= users.stream()
           .collect(Collectors.toMap(
               User::getId,
               user -> getUserApplicationRoles(user.getId(), kClientId)));

        return users
            .stream()
            .filter(user -> filterByApplication(userAppRoles.get(user.getId())))
            .filter(user -> filterByRoles(userAppRoles.get(user.getId()), userSearchParams.getRoles()))
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
        List<Group> groups = keycloakClient
            .searchGroups(groupSearchParams.getSearch(), PAGE_START, PAGE_SIZE)
            .stream()
            .map(keycloakGroupToGroup::toGroup)
            .collect(Collectors.toList());

        if(!StringUtils.isEmpty(groupSearchParams.getApplication())) {
            return filterGroupsInApplicationsScope(groups, groupSearchParams);
        } else {
            return filterGroupsInRealmScope(groups, groupSearchParams);
        }
    }

    private List<Group> filterGroupsInRealmScope(List<Group> groups, GroupSearchParams groupSearchParams) {
        Map<String, List<Role>> groupsRolesMapping = new HashMap<>();
        if(!CollectionUtils.isEmpty(groupSearchParams.getRoles())) {
            groups.forEach(group ->
                groupsRolesMapping.put(group.getId(),
                    getGroupRealmRoles(group.getId())));
        }
        return groups
            .stream()
            .filter(group -> filterByRoles(groupsRolesMapping.get(group.getId()),
                groupSearchParams.getRoles()))
            .collect(Collectors.toList());
    }

    private List<Role> getGroupRealmRoles(String groupId) {
        return keycloakRoleMappingToRole.toRoles(
            keycloakClient.getGroupRoleMapping(groupId));
    }

    private List<Group> filterGroupsInApplicationsScope(List<Group> groups ,GroupSearchParams userSearchParams) {
        String application = userSearchParams.getApplication();
        String kClientId = getKeycloakClientId(application);
        if(StringUtils.isEmpty(kClientId)) {
            return Collections.emptyList();
        }

        Map<String, List<Role>> groupAppRoles = groups.stream()
            .collect(Collectors.toMap(
                Group::getId,
                group -> getGroupApplicationRoles(group.getId(), kClientId)));

        return groups
            .stream()
            .filter(group -> filterByApplication(groupAppRoles.get(group.getId())))
            .filter(group -> filterByRoles(groupAppRoles.get(group.getId()), userSearchParams.getRoles()))
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

    private boolean filterByApplication(List<Role> applicationRoles) {
        return applicationRoles
            .stream()
            .findAny()
            .isPresent();
    }

    private List<Role> getUserApplicationRoles(String userId, String clientId) {
        if (!clientId.isEmpty()) {
            return keycloakRoleMappingToRole
                .toRoles(keycloakClient.getUserClientRoleMapping(userId, clientId));
        }
        return Collections.emptyList();
    }

    private List<Role> getGroupApplicationRoles(String groupId, String clientId) {
        if (!clientId.isEmpty()) {
            return keycloakRoleMappingToRole
                .toRoles(keycloakClient.getGroupClientRoleMapping(groupId, clientId));
        }
        return Collections.emptyList();
    }

    private String getKeycloakClientId(String application) {
        List<KeycloakClientRepresentation> kClients = keycloakClient.searchClients(application, 0, 1);
        return !kClients.isEmpty() ? kClients.get(0).getId() : null;
    }
}
