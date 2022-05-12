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
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakUserToUser;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
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

    @BeforeEach
    public void setupData() {
        Role roleA = new Role();
        roleA.setName("a");
        Role roleB = new Role();
        roleB.setName("b");

        userOne.setId("one");
        userOne.setRoles(List.of(roleB));

        userTwo.setId("two");
        userTwo.setRoles(List.of(roleA, roleB));

        userThree.setId("three");
        userThree.setRoles(List.of(roleA));

        groupOne.setId("one");
        groupOne.setName("one");
        groupOne.setRoles(List.of(roleB));

        groupTwo.setId("two");
        groupTwo.setName("one");
        groupTwo.setRoles(List.of(roleA, roleB));

        groupThree.setId("three");
        groupThree.setName("one");
        groupThree.setRoles(List.of(roleA));
    }

    @Test
    void shouldReturnUsersWhenSearchingUsingOneRole() {
        describeSearchUsers();
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
        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of("a", "b"));
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(3);
        assertThat(users).containsExactly(userOne, userTwo, userThree);
    }

    @Test
    void shouldReturnUsersWhenSearchingUsingGroupAndRoles() {
        describeSearchUsers();
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
    void shouldReturnAllUsersWhenSearchingWithoutRoles() {
        describeSearchUsers();

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch("o");
        userSearchParams.setRoles(Set.of());
        List<User> users = keycloakManagementService.findUsers(userSearchParams);
        assertThat(users.size()).isEqualTo(4);
        assertThat(users).containsExactly(userOne, userTwo, userThree, userFour);
    }

    @Test
    void shouldReturnGroupsWhenSearchingUsingOneRole() {
        describeSearchGroups();

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
        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch("o");
        groupSearchParams.setRoles(Set.of("b", "a"));
        List<Group> groups = keycloakManagementService.findGroups(groupSearchParams);
        assertThat(groups.size()).isEqualTo(3);
        assertThat(groups).containsExactly(groupOne, groupTwo, groupThree);
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

    private void describeSearchGroups() {
        when(keycloakClient.searchGroups(eq("o"), eq(0), eq(50))).thenReturn(List.of(new KeycloakGroup(), new KeycloakGroup(), new KeycloakGroup(), new KeycloakGroup()));
        when(keycloakGroupToGroup.toGroup(any())).thenReturn(groupOne, groupTwo, groupThree, groupFour);
    }

    private void describeSearchUsers() {
        when(keycloakClient.searchUsers(eq("o"), eq(0), eq(50))).thenReturn(List.of(new KeycloakUser(), new KeycloakUser(), new KeycloakUser(), new KeycloakUser()));
        when(keycloakUserToUser.toUser(any())).thenReturn(userOne, userTwo, userThree, userFour);
    }

}
