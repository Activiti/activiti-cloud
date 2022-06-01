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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.activiti.cloud.identity.GroupSearchParams;
import org.activiti.cloud.identity.UserSearchParams;
import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.Role;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakGroupToGroup;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakRoleMappingToRole;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakUserToUser;
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

    @Mock
    private KeycloakUserToUser keycloakUserToUser;

    @Mock
    private KeycloakGroupToGroup keycloakGroupToGroup;

    @Mock
    private KeycloakRoleMappingToRole keycloakRoleMappingToRole;

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
    public void setupData() {
        keycloakRoleA.setName("a");
        keycloakRoleB.setName("b");
        roleA.setId("a");
        roleA.setName("a");
        roleB.setName("b");
        roleB.setId("b");

        userOne.setId("one");
        userTwo.setId("two");
        userThree.setId("three");
        userFour.setId("four");

        groupOne.setId("one");
        groupOne.setName("one");

        groupTwo.setId("two");
        groupTwo.setName("two");

        groupThree.setId("three");
        groupThree.setName("three");

        groupFour.setId("four");

        clientOne.setId("one");
        clientOne.setClientId("client-one");
    }

    @Test
    void shouldReturnUsersWhenSearchingUsingOneRole() {
        describeSearchUsers();
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
        describeSearchUsers();
        setUpUsersRealmRoles();
        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of("a", "b"));
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userTwo);
    }

    @Test
    void shouldReturnUsersWhenSearchingUsingGroupAndRoles() {
        describeSearchUsers();
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
        describeSearchUsers();
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
        describeSearchUsers();
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
        describeSearchUsers();
        setupUsersApplicationRoles();

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setApplication("client-one");
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(2);
        assertThat(users).containsExactly(userOne, userTwo);
    }

    @Test
    void shouldReturnOnlyApplicationUsersWhenSearchingUsingApplicationAndGroups() {
        describeSearchUsers();
        setupUsersApplicationRoles();

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
        describeSearchUsers();
        setupUsersApplicationRoles();

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of("b"));
        userSearchParams.setApplication("client-one");
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users).containsExactly(userTwo);
    }

    @Test
    void shouldReturnOnlyApplicationUsersWhenSearchingUsingApplicationAndRolesAndGroups() {
        describeSearchUsers();
        setupUsersApplicationRoles();

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
    void shouldReturnGroupsWhenSearchingUsingOneRole() {
        describeSearchGroups();
        setUpGroupsRealmRoles();

        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setRoles(Set.of("a"));
        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);
        assertThat(groups.size()).isEqualTo(2);
        assertThat(groups).containsExactly(groupTwo, groupThree);
    }

    @Test
    void shouldReturnGroupsWhenSearchingUsingMultipleRoles() {
        describeSearchGroups();
        setUpGroupsRealmRoles();

        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setRoles(Set.of("b", "a"));
        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);
        assertThat(groups.size()).isEqualTo(1);
        assertThat(groups).containsExactly(groupTwo);
    }

    @Test
    void shouldReturnAllGroupsWhenSearchingWithoutRoles() {
        describeSearchGroups();

        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setRoles(Set.of());
        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);
        assertThat(groups.size()).isEqualTo(4);
        assertThat(groups).containsExactly(groupOne, groupTwo, groupThree, groupFour);
    }

    @Test
    void shouldReturnGroupsWhenSearchingUsingApplication() {
        describeSearchGroups();
        setupGroupsApplicationRoles();

        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setApplication("client-one");
        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);
        assertThat(groups.size()).isEqualTo(2);
        assertThat(groups).containsExactly(groupOne, groupTwo);
    }

    @Test
    void shouldReturnGroupsWhenSearchingUsingApplicationAndRoles() {
        describeSearchGroups();
        setupGroupsApplicationRoles();

        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setRoles(Set.of("b"));
        groupSearchParams.setApplication("client-one");
        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);
        assertThat(groups.size()).isEqualTo(1);
        assertThat(groups).containsExactly(groupTwo);
    }

    private void describeSearchGroups() {
        when(keycloakClient.searchGroups(eq("o"), eq(0), eq(50))).thenReturn(List.of(new KeycloakGroup(), new KeycloakGroup(), new KeycloakGroup(), new KeycloakGroup()));
        when(keycloakGroupToGroup.toGroup(any())).thenReturn(groupOne, groupTwo, groupThree, groupFour);
    }

    private void describeSearchUsers() {
        when(keycloakClient.searchUsers(eq("o"), eq(0), eq(50))).thenReturn(List.of(new KeycloakUser(), new KeycloakUser(), new KeycloakUser(), new KeycloakUser()));
        when(keycloakUserToUser.toUser(any())).thenReturn(userOne, userTwo, userThree, userFour);
    }

    private void setupUsersApplicationRoles() {
        when(keycloakClient.searchClients("client-one",0, 1)).thenReturn(List.of(clientOne));

        when(keycloakClient.getUserClientRoleMapping(userOne.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListA);
        when(keycloakClient.getUserClientRoleMapping(groupTwo.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListAB);

        when(keycloakRoleMappingToRole.toRoles(keycloakRolesListA)).thenReturn(List.of(roleA));
        when(keycloakRoleMappingToRole.toRoles(keycloakRolesListAB)).thenReturn(List.of(roleA, roleB));
    }

    private void setupGroupsApplicationRoles() {
        when(keycloakClient.searchClients("client-one",0, 1)).thenReturn(List.of(clientOne));

        when(keycloakClient.getGroupClientRoleMapping(groupOne.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListA);
        when(keycloakClient.getGroupClientRoleMapping(groupTwo.getId(), clientOne.getId()))
            .thenReturn(keycloakRolesListAB);

        when(keycloakRoleMappingToRole.toRoles(eq(keycloakRolesListA))).thenReturn(List.of(roleA));
        when(keycloakRoleMappingToRole.toRoles(eq(keycloakRolesListAB))).thenReturn(List.of(roleA, roleB));
    }

    private void setUpGroupsRealmRoles(){
        lenient().when(keycloakClient.getGroupRoleMapping(groupOne.getId())).thenReturn(keycloakRolesListB);
        lenient().when(keycloakClient.getGroupRoleMapping(groupTwo.getId())).thenReturn(keycloakRolesListAB);
        lenient().when(keycloakClient.getGroupRoleMapping(groupThree.getId())).thenReturn(keycloakRolesListA);

        lenient().when(keycloakRoleMappingToRole.toRoles(eq(keycloakRolesListB))).thenReturn(List.of(roleB));
        lenient().when(keycloakRoleMappingToRole.toRoles(eq(keycloakRolesListAB))).thenReturn(List.of(roleA, roleB));
        lenient().when(keycloakRoleMappingToRole.toRoles(eq(keycloakRolesListA))).thenReturn(List.of(roleA));
    }

    private void setUpUsersRealmRoles(){
        lenient().when(keycloakClient.getUserRoleMapping(userOne.getId())).thenReturn(keycloakRolesListB);
        lenient().when(keycloakClient.getUserRoleMapping(userTwo.getId())).thenReturn(keycloakRolesListAB);
        lenient().when(keycloakClient.getUserRoleMapping(userThree.getId())).thenReturn(keycloakRolesListA);

        lenient().when(keycloakRoleMappingToRole.toRoles(eq(keycloakRolesListB))).thenReturn(List.of(roleB));
        lenient().when(keycloakRoleMappingToRole.toRoles(eq(keycloakRolesListAB))).thenReturn(List.of(roleA, roleB));
        lenient().when(keycloakRoleMappingToRole.toRoles(eq(keycloakRolesListA))).thenReturn(List.of(roleA));
    }

}
