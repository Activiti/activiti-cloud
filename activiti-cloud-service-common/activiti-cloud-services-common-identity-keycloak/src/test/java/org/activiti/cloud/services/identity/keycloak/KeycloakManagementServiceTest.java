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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.activiti.cloud.identity.GroupSearchParams;
import org.activiti.cloud.identity.UserSearchParams;
import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.Role;
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
        groupTwo.setId("two");
        groupThree.setId("three");
        groupFour.setId("four");

        clientOne.setId("one");
        clientOne.setClientId("client-one");
    }

    @Test
    void shouldReturnUsersWhenSearchingUsingOneRole() {
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
    void shouldReturnUsersWhenSearchingUsingMultipleRoles() {
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

        KeycloakGroup one = new KeycloakGroup();
        one.setId("one");
        one.setName("one");
        when(keycloakClient.getUserGroups(userTwo.getId())).thenReturn(List.of(one));

        KeycloakGroup two = new KeycloakGroup();
        two.setId("two");
        two.setName("two");
        when(keycloakClient.getUserGroups(userThree.getId())).thenReturn(List.of(two));

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setGroups(Set.of("one"));
        userSearchParams.setRoles(Set.of("a"));

        List<User> users = keycloakManagementService.findUsers(userSearchParams);

        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userTwo);
    }

    @Test
    void shouldReturnUsersWhenSearchingUsingMultipleGroups() {
        defineSearchUsersFromKeycloak();
        KeycloakGroup one = new KeycloakGroup();
        one.setId("one");
        one.setName("one");
        KeycloakGroup two = new KeycloakGroup();
        two.setId("two");
        two.setName("two");
        when(keycloakClient.getUserGroups(userOne.getId())).thenReturn(List.of(one));
        when(keycloakClient.getUserGroups(userTwo.getId())).thenReturn(List.of(one, two));

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setGroups(Set.of("one", "two"));
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

        KeycloakGroup groupOne = new KeycloakGroup();
        groupOne.setId("one");
        groupOne.setName("one");
        when(keycloakClient.getUserGroups(userOne.getId())).thenReturn(List.of(groupOne));

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setApplication("client-one");
        userSearchParams.setGroups(Set.of("one"));
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

        KeycloakGroup kGroupOne = new KeycloakGroup();
        kGroupOne.setId("one");
        kGroupOne.setName("one");
        KeycloakGroup kGroupTwo = new KeycloakGroup();
        kGroupTwo.setId("two");
        kGroupTwo.setName("two");

        when(keycloakClient.getUserGroups(userOne.getId())).thenReturn(List.of(kGroupOne));
        when(keycloakClient.getUserGroups(userTwo.getId())).thenReturn(List.of(kGroupTwo));

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of("a"));
        userSearchParams.setApplication("client-one");
        userSearchParams.setGroups(Set.of("one"));

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
        KeycloakGroup kGroupOne = new KeycloakGroup();
        KeycloakGroup kGroupTwo = new KeycloakGroup();
        KeycloakGroup kGroupThree = new KeycloakGroup();
        KeycloakGroup kGroupFour = new KeycloakGroup();

        kGroupOne.setId("one");
        kGroupTwo.setId("two");
        kGroupThree.setId("three");
        kGroupFour.setId("four");

        when(keycloakClient.searchGroups(eq("o"), eq(0), eq(50)))
            .thenReturn(List.of(kGroupOne, kGroupTwo, kGroupThree, kGroupFour));
    }

    private void defineSearchUsersFromKeycloak() {
        KeycloakUser kUserOne = new KeycloakUser();
        KeycloakUser kUserTwo = new KeycloakUser();
        KeycloakUser kUserThree = new KeycloakUser();
        KeycloakUser kUserFour = new KeycloakUser();

        kUserOne.setId("one");
        kUserTwo.setId("two");
        kUserThree.setId("three");
        kUserFour.setId("four");

        when(keycloakClient.searchUsers(eq("o"), eq(0), eq(50)))
            .thenReturn(List.of(kUserOne, kUserTwo, kUserThree, kUserFour));
    }

    private void setUpUsersApplicationRoles() {
        when(keycloakClient.searchClients("client-one", 0, 1))
            .thenReturn(List.of(clientOne));
        when(keycloakClient.getUserClientRoleMapping(userOne.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListA);
        when(keycloakClient.getUserClientRoleMapping(groupTwo.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListAB);
    }

    private void setUpGroupsApplicationRoles() {
        when(keycloakClient.searchClients("client-one", 0, 1))
            .thenReturn(List.of(clientOne));
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

}
