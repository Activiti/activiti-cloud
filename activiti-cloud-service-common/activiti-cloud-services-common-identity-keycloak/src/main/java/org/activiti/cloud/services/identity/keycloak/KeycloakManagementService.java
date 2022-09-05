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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.cloud.identity.GroupSearchParams;
import org.activiti.cloud.identity.IdentityManagementService;
import org.activiti.cloud.identity.UserSearchParams;
import org.activiti.cloud.identity.exceptions.IdentityInvalidApplicationException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidGroupException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidGroupRoleException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidRoleException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidUserException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidUserRoleException;
import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.Role;
import org.activiti.cloud.identity.model.SecurityRequestBodyRepresentation;
import org.activiti.cloud.identity.model.SecurityResponseRepresentation;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.identity.model.UserRoles;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakGroupToGroup;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakRoleMappingToRole;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakTokenToUserRoles;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakUserToUser;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakClientRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

public class KeycloakManagementService implements IdentityManagementService {

    public static final int PAGE_START = 0;
    public static final int PAGE_SIZE = 50;

    private final KeycloakClient keycloakClient;

    public KeycloakManagementService(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @Override
    public List<User> findUsers(UserSearchParams userSearchParams) {
        List<User> users = ObjectUtils.isEmpty(userSearchParams.getGroups())
            ? searchUsers(userSearchParams.getSearchKey())
            : searchUsers(userSearchParams.getGroups(), userSearchParams.getSearchKey());

        if(!StringUtils.isEmpty(userSearchParams.getApplication())) {
            return filterUsersInApplicationsScope(users, userSearchParams);
        } else {
            return filterUsersInRealmScope(users, userSearchParams);
        }
    }

    private List<User> searchUsers(String searchKey) {
        return keycloakClient
            .searchUsers(searchKey, PAGE_START, PAGE_SIZE)
            .stream()
            .map(KeycloakUserToUser::toUser)
            .collect(Collectors.toList());
    }

    private List<User> searchUsers(Set<String> groups, String searchKey) {
        Predicate<User> maybeMatchSearchKey = user -> !StringUtils.isEmpty(searchKey)
            ? StringUtils.contains(user.getUsername(), searchKey) || StringUtils.contains(user.getEmail(), searchKey)
            : true;

        return groups.stream()
                     .map(this::findUsersByGroupName)
                     .flatMap(Collection::stream)
                     .distinct()
                     .filter(maybeMatchSearchKey)
                     .collect(Collectors.toList());
    }

    private List<User> filterUsersInRealmScope(List<User> users,
        UserSearchParams userSearchParams) {

        Map<String, List<Role>> usersRolesMapping = new HashMap<>();
        if (!CollectionUtils.isEmpty(userSearchParams.getRoles())) {
            mapUserWithRealmRoles(users, usersRolesMapping);
        }
        return users
            .stream()
            .filter(user -> filterByRoles(usersRolesMapping.get(user.getId()), userSearchParams.getRoles()))
            .collect(Collectors.toList());
    }

    private void mapUserWithRealmRoles(List<User> users, Map<String, List<Role>> usersRolesMapping) {
        users.forEach(user ->
            usersRolesMapping.put(user.getId(),
                getUserRealmRoles(user.getId())));
    }

    private List<Role> getUserRealmRoles(String userId) {
        return KeycloakRoleMappingToRole.toRoles(
                    keycloakClient.getUserRoleMapping(userId));
    }

    private List<User> filterUsersInApplicationsScope(List<User> users ,UserSearchParams userSearchParams) {
        String application = userSearchParams.getApplication();
        String kClientId = getKeycloakClientId(application);
        if(StringUtils.isEmpty(kClientId)) {
            return Collections.emptyList();
        }
        Map<String, List<Role>> userAppRoles= mapUsersWithApplicationRoles(users, kClientId);

        return users
            .stream()
            .filter(user -> filterByApplication(userAppRoles.get(user.getId())))
            .filter(user -> filterByRoles(userAppRoles.get(user.getId()), userSearchParams.getRoles()))
            .collect(Collectors.toList());
    }

    private Map<String, List<Role>>  mapUsersWithApplicationRoles(List<User> users, String kClientId) {
        return users.stream()
            .collect(Collectors.toMap(
                User::getId,
                user -> getUserApplicationRoles(user.getId(), kClientId)));
    }

    @Override
    public List<Group> findGroups(GroupSearchParams groupSearchParams) {
        List<Group> groups = searchGroups(groupSearchParams.getSearch());

        if(!StringUtils.isEmpty(groupSearchParams.getApplication())) {
            return filterGroupsInApplicationsScope(groups, groupSearchParams);
        } else {
            return filterGroupsInRealmScope(groups, groupSearchParams);
        }
    }

    private List<Group> searchGroups(String searchKey) {
        return keycloakClient
            .searchGroups(searchKey, PAGE_START, PAGE_SIZE)
            .stream()
            .map(KeycloakGroupToGroup::toGroup)
            .collect(Collectors.toList());
    }

    private List<Group> filterGroupsInRealmScope(List<Group> groups, GroupSearchParams groupSearchParams) {
        Map<String, List<Role>> groupsRolesMapping = new HashMap<>();
        if(!CollectionUtils.isEmpty(groupSearchParams.getRoles())) {
            mapGroupsWithRealmRoles(groups, groupsRolesMapping);
        }
        return groups
            .stream()
            .filter(group -> filterByRoles(groupsRolesMapping.get(group.getId()),
                groupSearchParams.getRoles()))
            .collect(Collectors.toList());
    }

    private void mapGroupsWithRealmRoles(List<Group> groups, Map<String, List<Role>> groupsRolesMapping) {
        groups.forEach(group ->
            groupsRolesMapping.put(group.getId(),
                getGroupRealmRoles(group.getId())));
    }

    private List<Role> getGroupRealmRoles(String groupId) {
        return KeycloakRoleMappingToRole.toRoles(
            keycloakClient.getGroupRoleMapping(groupId));
    }

    private List<Group> filterGroupsInApplicationsScope(List<Group> groups ,GroupSearchParams userSearchParams) {
        String application = userSearchParams.getApplication();
        String kClientId = getKeycloakClientId(application);
        if(StringUtils.isEmpty(kClientId)) {
            return Collections.emptyList();
        }
        Map<String, List<Role>> groupAppRoles =  mapGroupsWithApplicationRoles(groups, kClientId);

        return groups
            .stream()
            .filter(group -> filterByApplication(groupAppRoles.get(group.getId())))
            .filter(group -> filterByRoles(groupAppRoles.get(group.getId()), userSearchParams.getRoles()))
            .collect(Collectors.toList());
    }

    private Map<String, List<Role>> mapGroupsWithApplicationRoles(List<Group> groups, String kClientId) {
        return groups
            .stream()
            .collect(Collectors.toMap(
                Group::getId,
                group -> getGroupApplicationRoles(group.getId(), kClientId)));
    }

    private boolean filterByRoles(List<Role> currentRoles, Set<String> filterRoles) {
        return CollectionUtils.isEmpty(filterRoles) ||
            currentRoles != null && currentRoles
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet())
                .containsAll(filterRoles);
    }

    @Override
    public UserRoles getUserRoles(Jwt principal) {
        return KeycloakTokenToUserRoles.toUserRoles(principal);
    }

    @Override
    public List<SecurityResponseRepresentation> getApplicationPermissions(String application, Set<String> roles) {
        String clientId = getKeycloakClientId(application);
        Set<String> applicationRolesFilter = getApplicationRolesToSearch(roles, clientId);
        List<SecurityResponseRepresentation> applicationPermissions = new ArrayList<>();

        applicationRolesFilter.forEach( role -> {
            SecurityResponseRepresentation securityResponseRepresentation = new SecurityResponseRepresentation();
            securityResponseRepresentation.setRole(role);
            securityResponseRepresentation.setUsers(getUsersClientRoleMapping(clientId, role));
            securityResponseRepresentation.setGroups(getGroupsClientRoleMapping(clientId, role));
            applicationPermissions.add(securityResponseRepresentation);
        });

        return applicationPermissions;
    }

    @Override
    public List<User> findUsersByGroupName(String groupName) {
        Group groupFound = findGroupStrictlyEqualToGroupName(groupName);
        return getUsersByGroupId(groupFound.getId());
    }

    private Group findGroupStrictlyEqualToGroupName(String groupName) {
        return
            Optional.ofNullable(groupName)
                .filter(Predicate.not(String::isEmpty))
                .map(g -> searchGroups(g).stream())
                .orElse(Stream.empty())
                .filter(group -> group.getName().equals(groupName))
                .findFirst()
                .orElseThrow(() -> new IdentityInvalidGroupException(groupName));
    }

    @Override
    public User findUserByName(String userName) {
        Predicate<User> username = user -> user.getUsername().equalsIgnoreCase(userName);
        Predicate<User> email = user -> user.getEmail().equalsIgnoreCase(userName);

        return searchUsers(userName)
            .stream()
            .filter(username.or(email))
            .findFirst()
            .orElseThrow();
    }

    @Override
    public Group findGroupByName(String groupName) {
        return searchGroups(groupName)
            .stream()
            .filter(group -> group.getName().equalsIgnoreCase(groupName))
            .findFirst()
            .orElseThrow();
    }

    private List<User> getUsersByGroupId(String groupID) {
        return
            keycloakClient.getUsersByGroupId(groupID)
                .stream()
                .map(KeycloakUserToUser::toUser)
                .collect(Collectors.toList());
    }

    private List<User> getUsersClientRoleMapping(String clientId, String role) {
        return keycloakClient.getUsersClientRoleMapping(clientId, role)
            .stream()
            .map(KeycloakUserToUser::toUser)
            .collect(Collectors.toList());
    }

    private List<Group> getGroupsClientRoleMapping(String clientId, String role) {
        return keycloakClient.getGroupsClientRoleMapping(clientId, role)
            .stream()
            .map(KeycloakGroupToGroup::toGroup)
            .collect(Collectors.toList());
    }

    private Set<String> getApplicationRolesToSearch(Set<String> roles, String clientId) {
        return keycloakClient
            .getClientRoles(clientId)
            .stream()
            .map(KeycloakRoleMapping::getName)
            .filter(clientRole -> CollectionUtils.isEmpty(roles) || roles.contains(clientRole))
            .collect(Collectors.toSet());
    }

    @Override
    public void addApplicationPermissions(String application, List<SecurityRequestBodyRepresentation> securityRequestBodyRepresentations) {
        String clientId = getKeycloakClientId(application);
        if(StringUtils.isEmpty(clientId)) {
            throw new IdentityInvalidApplicationException(application);
        }
        securityRequestBodyRepresentations.forEach(securityRepresentation -> {
            String roleName = securityRepresentation.getRole();
            KeycloakRoleMapping keycloakRoleMapping = getKeyCloakRoleFromRoleName(roleName, clientId);

            List<String> validatedUsers = new ArrayList<>();
            List<String> validatedGroups = new ArrayList<>();

            if(securityRepresentation.getUsers() != null) {
                securityRepresentation.getUsers().forEach(
                    username -> validatedUsers.add(validateUserApplicationPermissions(username, roleName)));
            }
            if(securityRepresentation.getGroups() != null) {
                securityRepresentation.getGroups().forEach(
                    groupName -> validatedGroups
                        .add(validateGroupApplicationPermissions(groupName, roleName)));
            }
            addApplicationRolePermissions(keycloakRoleMapping, validatedUsers, validatedGroups, clientId);
        });
    }

    private void addApplicationRolePermissions(KeycloakRoleMapping keycloakRoleMapping, List<String> usersId,
        List<String> groupsId, String clientId) {

        usersId.forEach(userId ->
            keycloakClient.addUserClientRoleMapping(userId, clientId,List.of(keycloakRoleMapping)));

        groupsId.forEach(groupId ->
            keycloakClient.addGroupClientRoleMapping(groupId, clientId,List.of(keycloakRoleMapping)));
    }

    private KeycloakRoleMapping getKeyCloakRoleFromRoleName(String roleName, String clientId) {
        if(roleName == null ) {
            throw new IdentityInvalidRoleException();
        }
        return keycloakClient.getClientRoles(clientId)
            .stream()
            .filter(kRole -> kRole.getName().equals(roleName))
            .findFirst()
            .orElseThrow(
                () -> new IdentityInvalidRoleException(roleName));
    }

    private String validateUserApplicationPermissions(String username, String roleName) {
        User user = getUserFromUsername(username);
        if(!userHasRole(user.getId(), roleName)) {
            throw new IdentityInvalidUserRoleException(username, roleName);
        }
        return user.getId();
    }

    private User getUserFromUsername(String username) {
        return searchUsers(username)
            .stream()
            .filter(u -> u.getUsername().equals(username))
            .findFirst()
            .orElseThrow(() -> new IdentityInvalidUserException(username));
    }

    private boolean userHasRole (String userId, String role) {
        return getUserRealmRoles(userId)
            .stream()
            .anyMatch(userRole -> userRole.getName().equals(role));
    }

    private String validateGroupApplicationPermissions(String groupName, String roleName ) {
        Group group = getGroupFromGroupName(groupName);

        if(!groupHasRole(group.getId(), roleName)) {
            throw new IdentityInvalidGroupRoleException(groupName, roleName);
        }
        return group.getId();
    }

    private Group getGroupFromGroupName(String groupName) {
        return searchGroups(groupName)
            .stream()
            .filter(g -> g.getName().equals(groupName))
            .findFirst()
            .orElseThrow(
                () -> new IdentityInvalidGroupException(groupName));
    }

    private boolean groupHasRole (String groupId, String role) {
        return getGroupRealmRoles(groupId)
            .stream()
            .anyMatch(groupRole -> groupRole.getName().equals(role));
    }

    private boolean filterByApplication(List<Role> applicationRoles) {
        return applicationRoles
            .stream()
            .findAny()
            .isPresent();
    }

    private List<Role> getUserApplicationRoles(String userId, String clientId) {
        if (!clientId.isEmpty()) {
            return KeycloakRoleMappingToRole
                .toRoles(keycloakClient.getUserClientRoleMapping(userId, clientId));
        }
        return Collections.emptyList();
    }

    private List<Role> getGroupApplicationRoles(String groupId, String clientId) {
        if (!clientId.isEmpty()) {
            return KeycloakRoleMappingToRole
                .toRoles(keycloakClient.getGroupClientRoleMapping(groupId, clientId));
        }
        return Collections.emptyList();
    }

    private String getKeycloakClientId(String application) {
        List<KeycloakClientRepresentation> kClients = keycloakClient.searchClients(application, 0, 1);
        return !kClients.isEmpty() ? kClients.get(0).getId() : null;
    }
}
