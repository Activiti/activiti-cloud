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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.activiti.cloud.identity.GroupSearchParams;
import org.activiti.cloud.identity.UserSearchParams;
import org.activiti.cloud.identity.exceptions.IdentityInvalidApplicationException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidGroupException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidGroupRoleException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidRoleException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidUserException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidUserRoleException;
import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.Role;
import org.activiti.cloud.identity.model.SecurityResponseRepresentation;
import org.activiti.cloud.identity.model.SecurityRequestBodyRepresentation;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakClientRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakManagementServiceTest {

    @Mock
    private KeycloakClient keycloakClient;

    @InjectMocks
    private KeycloakManagementService keycloakManagementService;

    private User userOne = new User();
    private User userTwo = new User();
    private User userThree = new User();
    private User userFour = new User();

    private Group groupOne = new Group();
    private Group groupTwo = new Group();
    private Group groupThree = new Group();
    private Group groupFour = new Group();

    private Role roleA = new Role();
    private Role roleB = new Role();

    private KeycloakRoleMapping keycloakRoleA = new KeycloakRoleMapping();
    private KeycloakRoleMapping keycloakRoleB = new KeycloakRoleMapping();
    private List<KeycloakRoleMapping> keycloakRolesListA = List.of(keycloakRoleA);
    private List<KeycloakRoleMapping> keycloakRolesListAB = List.of(keycloakRoleA, keycloakRoleB);
    private List<KeycloakRoleMapping> keycloakRolesListB = List.of(keycloakRoleB);

    private KeycloakClientRepresentation clientOne = new KeycloakClientRepresentation();

    private KeycloakGroup kGroupOne = new KeycloakGroup();
    private KeycloakGroup kGroupTwo = new KeycloakGroup();
    private KeycloakGroup kGroupThree = new KeycloakGroup();
    private KeycloakGroup kGroupFour = new KeycloakGroup();

    private KeycloakUser kUserOne = new KeycloakUser();
    private KeycloakUser kUserTwo = new KeycloakUser();
    private KeycloakUser kUserThree = new KeycloakUser();
    private KeycloakUser kUserFour = new KeycloakUser();

    @BeforeEach
    public void setUpData() {
        keycloakRoleA.setName("a");
        keycloakRoleB.setName("b");

        roleA.setId("a");
        roleB.setId("b");

        userOne.setId("one");
        userTwo.setId("two");
        userThree.setId("three");
        userFour.setId("four");

        groupOne.setId("one");
        groupOne.setName("groupOne");
        groupTwo.setId("two");
        groupThree.setId("three");
        groupFour.setId("four");

        clientOne.setId("one");
        clientOne.setClientId("client-one");

        kUserOne.setId("one");
        kUserOne.setUsername("userOne");
        kUserTwo.setId("two");
        kUserTwo.setUsername("userTwo");
        kUserThree.setId("three");
        kUserFour.setId("four");

        kGroupOne.setId("one");
        kGroupOne.setName("groupOne");
        kGroupTwo.setId("two");
        kGroupTwo.setName("groupTwo");
        kGroupThree.setId("three");
        kGroupFour.setId("four");
    }

    @Test
    void should_returnUsers_when_searchingUsingOneRole() {
        defineSearchUsersFromKeycloak();
        setUpUsersRealmRoles();
        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of("a"));

        List<User> users = keycloakManagementService.findUsers(userSearchParams);

        assertThat(users.size()).isEqualTo(2);
        assertThat(users).containsExactly(userTwo, userThree);
    }

    @Test
    void should_returnUsers_when_searchingUsingMultipleRoles() {
        defineSearchUsersFromKeycloak();
        setUpUsersRealmRoles();
        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of("a", "b"));

        List<User> users = keycloakManagementService.findUsers(userSearchParams);

        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userTwo);
    }

    @Test
    void should_returnUsers_when_searchingUsingGroupAndRoles() {
        defineSearchUsersFromKeycloak();
        setUpUsersRealmRoles();
        when(keycloakClient.getUserGroups(userTwo.getId())).thenReturn(List.of(kGroupOne));
        when(keycloakClient.getUserGroups(userThree.getId())).thenReturn(List.of(kGroupTwo));

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setGroups(Set.of("groupOne"));
        userSearchParams.setRoles(Set.of("a"));

        List<User> users = keycloakManagementService.findUsers(userSearchParams);

        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userTwo);
    }

    @Test
    void shouldReturnUsersWhenSearchingUsingMultipleGroups() {
        defineSearchUsersFromKeycloak();
        when(keycloakClient.getUserGroups(userOne.getId())).thenReturn(List.of(kGroupOne));
        when(keycloakClient.getUserGroups(userTwo.getId())).thenReturn(List.of(kGroupOne, kGroupTwo));

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setGroups(Set.of("groupOne", "groupTwo"));
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userTwo);
    }

    @Test
    void shouldReturnAllUsersWhenSearchingWithoutRoles() {
        defineSearchUsersFromKeycloak();
        setUpUsersRealmRoles();

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of());
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(4);
        assertThat(users).containsExactly(userOne, userTwo, userThree, userFour);
    }

    @Test
    void shouldReturnUsersWhenUsingApplication() {
        defineSearchUsersFromKeycloak();
        setUpUsersApplicationRoles();

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setApplication("client-one");
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(2);
        assertThat(users).containsExactly(userOne, userTwo);
    }

    @Test
    void shouldReturnOnlyApplicationUsersWhenSearchingUsingApplicationAndGroups() {
        defineSearchUsersFromKeycloak();
        setUpUsersApplicationRoles();
        when(keycloakClient.getUserGroups(userOne.getId())).thenReturn(List.of(kGroupOne));

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setApplication("client-one");
        userSearchParams.setGroups(Set.of("groupOne"));
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userOne);
    }

    @Test
    void shouldReturnOnlyApplicationUsersWhenSearchingUsingApplicationAndRoles() {
        defineSearchUsersFromKeycloak();
        setUpUsersApplicationRoles();

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of("b"));
        userSearchParams.setApplication("client-one");
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userTwo);
    }

    @Test
    void should_returnOnlyApplicationUsers_when_searchingUsingApplicationAndRolesAndGroups() {
        defineSearchUsersFromKeycloak();
        setUpUsersApplicationRoles();
        when(keycloakClient.getUserGroups(userOne.getId())).thenReturn(List.of(kGroupOne));
        when(keycloakClient.getUserGroups(userTwo.getId())).thenReturn(List.of(kGroupTwo));

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of("a"));
        userSearchParams.setApplication("client-one");
        userSearchParams.setGroups(Set.of("groupOne"));

        List<User> users = keycloakManagementService.findUsers(userSearchParams);

        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userOne);
    }

    @Test
    void should_returnGroups_when_searchingUsingOneRole() {
        defineSearchGroupsFromKeycloak();
        setUpGroupsRealmRoles();
        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setRoles(Set.of("a"));

        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);

        assertThat(groups.size()).isEqualTo(2);
        assertThatGroupsAreEqual(groups, Stream.of(groupTwo, groupThree));
    }

    @Test
    void should_returnGroups_when_searchingUsingMultipleRoles() {
        defineSearchGroupsFromKeycloak();
        setUpGroupsRealmRoles();

        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setRoles(Set.of("b", "a"));
        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);
        assertThat(groups.size()).isEqualTo(1);
        assertThatGroupsAreEqual(groups, Stream.of(groupTwo));
    }

    @Test
    void should_returnAllGroups_when_searchingWithoutRoles() {
        defineSearchGroupsFromKeycloak();
        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setRoles(Set.of());

        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);

        assertThat(groups.size()).isEqualTo(4);
        assertThatGroupsAreEqual(groups, Stream.of(groupOne, groupTwo, groupThree, groupFour));
    }

    @Test
    void should_returnGroups_when_searchingUsingApplication() {
        defineSearchGroupsFromKeycloak();
        setUpGroupsApplicationRoles();
        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setApplication("client-one");

        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);

        assertThat(groups.size()).isEqualTo(2);
        assertThatGroupsAreEqual(groups, Stream.of(groupOne, groupTwo));
    }

    @Test
    void should_returnGroups_when_searchingUsingApplicationAndRoles() {
        defineSearchGroupsFromKeycloak();
        setUpGroupsApplicationRoles();
        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setRoles(Set.of("b"));
        groupSearchParams.setApplication("client-one");

        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);

        assertThat(groups.size()).isEqualTo(1);
        assertThatGroupsAreEqual(groups, Stream.of(groupTwo));
    }

    @Test
    public void should_throwInvalidApplicationException_When_AddingPermissionAndApplicationIsInvalid () {
        String expectedMessage = "Invalid Security data: application {fakeClient} is invalid or doesn't exist";

        IdentityInvalidApplicationException exception = assertThrows(IdentityInvalidApplicationException.class,
            () -> keycloakManagementService.addApplicationPermissions(
                "fakeClient", List.of(new SecurityRequestBodyRepresentation())));
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    void should_throwIdentityInvalidRoleException_when_addingApplicationPermissionsWithInvalidRole() {
        String expectedMessage = "Invalid Security data: role {fakeRole} is invalid or doesn't exist";
        setUpClient();
        SecurityRequestBodyRepresentation securityRequestBodyRepresentation = new SecurityRequestBodyRepresentation();
        securityRequestBodyRepresentation.setRole("fakeRole");

        IdentityInvalidRoleException exception = assertThrows(IdentityInvalidRoleException.class,
            () -> keycloakManagementService.addApplicationPermissions("client-one",
                List.of(securityRequestBodyRepresentation)));

        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }


    @Test
    void should_throwIdentityInvalidUserException_when_addingApplicationPermissionsWithInvalidUser() {
        String expectedMessage = "Invalid Security data: user {fakeUser} is invalid or doesn't exist";
        setUpClient();
        when(keycloakClient.searchUsers(eq("fakeUser"), eq(0), eq(50)))
            .thenReturn(Collections.emptyList());
        when(keycloakClient.getClientRoles(clientOne.getId())).thenReturn(List.of(keycloakRoleA));

        SecurityRequestBodyRepresentation securityRequestBodyRepresentation = new SecurityRequestBodyRepresentation();
        securityRequestBodyRepresentation.setRole("a");
        securityRequestBodyRepresentation.setUsers(List.of("fakeUser"));

        IdentityInvalidUserException exception = assertThrows(IdentityInvalidUserException.class,
            () -> keycloakManagementService.addApplicationPermissions("client-one",
                List.of(securityRequestBodyRepresentation)));

        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    void should_throwIdentityInvalidUserRoleException_when_addingApplicationPermissionsWithInvalidUserRole() {
        String expectedMessage = "Invalid Security data: role {a} can't be assigned to user {userOne}";
        setUpClient();
        setUpUsersRealmRoles();
        when(keycloakClient.searchUsers(eq("userOne"), eq(0), eq(50)))
            .thenReturn(List.of(kUserOne));
        when(keycloakClient.getClientRoles(clientOne.getId())).thenReturn(List.of(keycloakRoleA));

        SecurityRequestBodyRepresentation securityRequestBodyRepresentation = new SecurityRequestBodyRepresentation();
        securityRequestBodyRepresentation.setRole("a");
        securityRequestBodyRepresentation.setUsers(List.of("userOne"));

        IdentityInvalidUserRoleException exception = assertThrows(IdentityInvalidUserRoleException.class,
            () -> keycloakManagementService.addApplicationPermissions("client-one",
                List.of(securityRequestBodyRepresentation)));

        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    void should_addApplicationPermissionsToUsers() {
        setUpClient();
        setUpUsersRealmRoles();
        when(keycloakClient.searchUsers(eq("userOne"), eq(0), eq(50)))
            .thenReturn(List.of(kUserOne));
        when(keycloakClient.getClientRoles(clientOne.getId())).thenReturn(List.of(keycloakRoleB));

        SecurityRequestBodyRepresentation securityRequestBodyRepresentation = new SecurityRequestBodyRepresentation();
        securityRequestBodyRepresentation.setRole("b");
        securityRequestBodyRepresentation.setUsers(List.of("userOne"));

        keycloakManagementService.addApplicationPermissions("client-one",
            List.of(securityRequestBodyRepresentation));

        verify(keycloakClient,times(1))
            .addUserClientRoleMapping(kUserOne.getId(),clientOne.getId(), List.of(keycloakRoleB));
    }

    @Test
    void should_notAddApplicationPermissionsToUsers_whenGroupIsInvalid() {
        String expectedMessage = "Invalid Security data: group {fakeGroup} is invalid or doesn't exist";
        setUpClient();
        setUpUsersRealmRoles();
        when(keycloakClient.searchUsers(eq("userOne"), eq(0), eq(50)))
            .thenReturn(List.of(kUserOne));
        when(keycloakClient.searchGroups(eq("fakeGroup"), eq(0), eq(50)))
            .thenReturn(Collections.emptyList());
        when(keycloakClient.getClientRoles(clientOne.getId())).thenReturn(List.of(keycloakRoleB));

        SecurityRequestBodyRepresentation securityRequestBodyRepresentation = new SecurityRequestBodyRepresentation();
        securityRequestBodyRepresentation.setRole("b");
        securityRequestBodyRepresentation.setUsers(List.of("userOne"));
        securityRequestBodyRepresentation.setGroups(List.of("fakeGroup"));

        IdentityInvalidGroupException exception = assertThrows(IdentityInvalidGroupException.class,
            () -> keycloakManagementService.addApplicationPermissions("client-one",
                List.of(securityRequestBodyRepresentation)));

        assertThat(exception.getMessage()).isEqualTo(expectedMessage);

        verify(keycloakClient,times(0))
            .addUserClientRoleMapping(kUserOne.getId(),clientOne.getId(), List.of(keycloakRoleB));
    }

    @Test
    void should_throwIdentityInvalidGroupException_when_addingApplicationPermissionsWithInvalidGroup() {
        String expectedMessage = "Invalid Security data: group {fakeGroup} is invalid or doesn't exist";
        setUpClient();
        when(keycloakClient.searchGroups(eq("fakeGroup"), eq(0), eq(50)))
            .thenReturn(Collections.emptyList());
        when(keycloakClient.getClientRoles(clientOne.getId())).thenReturn(List.of(keycloakRoleA));

        SecurityRequestBodyRepresentation securityRequestBodyRepresentation = new SecurityRequestBodyRepresentation();
        securityRequestBodyRepresentation.setRole("a");
        securityRequestBodyRepresentation.setGroups(List.of("fakeGroup"));

        IdentityInvalidGroupException exception = assertThrows(IdentityInvalidGroupException.class,
            () -> keycloakManagementService.addApplicationPermissions("client-one",
                List.of(securityRequestBodyRepresentation)));

        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    void should_throwIdentityInvalidGroupRoleException_when_addingApplicationPermissionsWithInvalidGroupRole() {
        String expectedMessage = "Invalid Security data: role {a} can't be assigned to group {groupOne}";
        setUpClient();
        setUpGroupsRealmRoles();

        when(keycloakClient.searchGroups(eq("groupOne"), eq(0), eq(50)))
            .thenReturn(List.of(kGroupOne));
        when(keycloakClient.getClientRoles(clientOne.getId())).thenReturn(List.of(keycloakRoleA));

        SecurityRequestBodyRepresentation securityRequestBodyRepresentation = new SecurityRequestBodyRepresentation();
        securityRequestBodyRepresentation.setRole("a");
        securityRequestBodyRepresentation.setGroups(List.of("groupOne"));

        IdentityInvalidGroupRoleException exception = assertThrows(IdentityInvalidGroupRoleException.class,
            () -> keycloakManagementService.addApplicationPermissions("client-one",
                List.of(securityRequestBodyRepresentation)));

        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    void should_addApplicationPermissionsToGroup() {
        setUpClient();
        setUpGroupsRealmRoles();
        when(keycloakClient.searchGroups(eq("groupOne"), eq(0), eq(50)))
            .thenReturn(List.of(kGroupOne));
        when(keycloakClient.getClientRoles(clientOne.getId())).thenReturn(List.of(keycloakRoleB));

        SecurityRequestBodyRepresentation securityRequestBodyRepresentation = new SecurityRequestBodyRepresentation();
        securityRequestBodyRepresentation.setRole("b");
        securityRequestBodyRepresentation.setGroups(List.of("groupOne"));

        keycloakManagementService.addApplicationPermissions("client-one",
            List.of(securityRequestBodyRepresentation));

        verify(keycloakClient,times(1))
            .addGroupClientRoleMapping(kGroupOne.getId(),clientOne.getId(), List.of(keycloakRoleB));
    }

    @Test
    void should_getApplicationPermissions() {
        setUpClient();
        when(keycloakClient.getClientRoles(clientOne.getId())).thenReturn(List.of(keycloakRoleA, keycloakRoleB));
        when(keycloakClient.getUsersClientRoleMapping(clientOne.getId(), keycloakRoleA.getName()))
            .thenReturn(List.of(kUserOne));
        when(keycloakClient.getUsersClientRoleMapping(clientOne.getId(), keycloakRoleB.getName()))
            .thenReturn(List.of(kUserTwo));
        when(keycloakClient.getGroupsClientRoleMapping(clientOne.getId(), keycloakRoleA.getName()))
            .thenReturn(List.of(kGroupOne));
        when(keycloakClient.getGroupsClientRoleMapping(clientOne.getId(), keycloakRoleB.getName()))
            .thenReturn(List.of(kGroupTwo));

        List<SecurityResponseRepresentation> securityRepresentations =
            keycloakManagementService.getApplicationPermissions(clientOne.getClientId(), Collections.emptySet());

        assertThat(securityRepresentations.size()).isEqualTo(2);
        assertThat(securityRepresentations)
            .extracting(SecurityResponseRepresentation::getRole,
                SecurityResponseRepresentation::getUsers,
                SecurityResponseRepresentation::getGroups)
            .contains(tuple(keycloakRoleA.getName(), List.of(userOne), List.of(groupOne)),
                tuple(keycloakRoleB.getName(), List.of(userTwo), List.of(groupTwo)));
    }

    @Test
    void should_getApplicationPermissions_when_filteringByRole() {
        setUpClient();
        when(keycloakClient.getClientRoles(clientOne.getId())).thenReturn(List.of(keycloakRoleA, keycloakRoleB));
        when(keycloakClient.getUsersClientRoleMapping(clientOne.getId(), keycloakRoleA.getName()))
            .thenReturn(List.of(kUserOne));
        when(keycloakClient.getGroupsClientRoleMapping(clientOne.getId(), keycloakRoleA.getName()))
            .thenReturn(List.of(kGroupOne));

        List<SecurityResponseRepresentation> securityRepresentations =
            keycloakManagementService.getApplicationPermissions(clientOne.getClientId(),
                Set.of(keycloakRoleA.getName()));

        assertThat(securityRepresentations.size()).isEqualTo(1);
        assertThat(securityRepresentations)
            .extracting(SecurityResponseRepresentation::getRole,
                SecurityResponseRepresentation::getUsers,
                SecurityResponseRepresentation::getGroups)
            .contains(tuple(keycloakRoleA.getName(), List.of(userOne), List.of(groupOne)));
    }

    @Test
    void should_returnUsers_when_searchingByGroupName() {
        when(keycloakClient.getUsersByGroupId(eq(groupOne.getId())))
            .thenReturn(List.of(kUserOne));
        when(keycloakClient.searchGroups(eq(groupOne.getName()), eq(0), eq(50)))
            .thenReturn(List.of(kGroupOne));

        List<User> users = keycloakManagementService.findUsersByGroupName(groupOne.getName());

        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userOne);
    }

    @Test
    void should_returnEmptyUserList_when_groupNameNotFound() {
        List<User> users = keycloakManagementService.findUsersByGroupName("fakeGroup");

        assertThat(users).isEmpty();
    }


    private void assertThatGroupsAreEqual(List<Group> groups, Stream<Group> groupsToCompare) {
        assertTrue(
            groupsToCompare.map(Group::getId)
                .allMatch(originalGroupId ->
                    groups
                        .stream()
                        .map(Group::getId)
                        .anyMatch(retrievedGroupId -> Objects.equals(retrievedGroupId, originalGroupId))));
    }

    private void defineSearchGroupsFromKeycloak() {
        when(keycloakClient.searchGroups(eq("o"), eq(0), eq(50)))
            .thenReturn(List.of(kGroupOne, kGroupTwo, kGroupThree, kGroupFour));
    }

    private void defineSearchUsersFromKeycloak() {
        when(keycloakClient.searchUsers(eq("o"), eq(0), eq(50)))
            .thenReturn(List.of(kUserOne, kUserTwo, kUserThree, kUserFour));
    }

    private void setUpUsersApplicationRoles() {
        setUpClient();
        when(keycloakClient.getUserClientRoleMapping(userOne.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListA);
        when(keycloakClient.getUserClientRoleMapping(userTwo.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListAB);
    }

    private void setUpGroupsApplicationRoles() {
        setUpClient();
        when(keycloakClient.getGroupClientRoleMapping(groupOne.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListA);
        when(keycloakClient.getGroupClientRoleMapping(groupTwo.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListAB);
    }

    private void setUpGroupsRealmRoles() {
        lenient().when(keycloakClient.getGroupRoleMapping(groupOne.getId())).thenReturn(keycloakRolesListB);
        lenient().when(keycloakClient.getGroupRoleMapping(groupTwo.getId())).thenReturn(keycloakRolesListAB);
        lenient().when(keycloakClient.getGroupRoleMapping(groupThree.getId())).thenReturn(keycloakRolesListA);
    }

    private void setUpUsersRealmRoles() {
        lenient().when(keycloakClient.getUserRoleMapping(userOne.getId())).thenReturn(keycloakRolesListB);
        lenient().when(keycloakClient.getUserRoleMapping(userTwo.getId())).thenReturn(keycloakRolesListAB);
        lenient().when(keycloakClient.getUserRoleMapping(userThree.getId())).thenReturn(keycloakRolesListA);
    }

    private void setUpClient() {
        when(keycloakClient.searchClients("client-one", 0, 1))
            .thenReturn(List.of(clientOne));
    }

}
