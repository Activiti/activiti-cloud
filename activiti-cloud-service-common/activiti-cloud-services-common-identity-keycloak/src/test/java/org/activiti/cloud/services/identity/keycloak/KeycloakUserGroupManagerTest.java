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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakUserGroupManagerTest {

    private static final int SEARCH_FIRST = 0;
    private static final int SEARCH_MAX = 2;

    @Mock
    private KeycloakClient keycloakClient;

    @InjectMocks
    private KeycloakUserGroupManager keycloakUserGroupManager;

    private KeycloakUser salesUser;

    @BeforeEach
    void setUp() {
        salesUser = keycloakUser("sales-user-id", "salesuser");
    }

    @Test
    void shouldReturnGroupNamesWhenSearchResolvesExactSingleUser() {
        when(keycloakClient.searchUsers("salesuser", SEARCH_FIRST, SEARCH_MAX)).thenReturn(List.of(salesUser));
        when(keycloakClient.getUserGroups("sales-user-id")).thenReturn(
            List.of(keycloakGroup("sales"), keycloakGroup("hr"))
        );

        List<String> groups = keycloakUserGroupManager.getUserGroups("salesuser");

        assertThat(groups).containsExactly("sales", "hr");
    }

    @Test
    void shouldThrowNotFoundWhenSearchReturnsNoUsers() {
        when(keycloakClient.searchUsers("superadminuser", SEARCH_FIRST, SEARCH_MAX)).thenReturn(List.of());

        assertThatThrownBy(() -> keycloakUserGroupManager.getUserGroups("superadminuser"))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("User id superadminuser not found");
    }

    @Test
    void shouldThrowNotUniqueWhenSearchReturnsMultipleUsers() {
        when(keycloakClient.searchUsers("superadminuser", SEARCH_FIRST, SEARCH_MAX)).thenReturn(
            List.of(keycloakUser("super-id", "superadminuser"), keycloakUser("admin-id", "superadminuser2"))
        );

        assertThatThrownBy(() -> keycloakUserGroupManager.getUserGroups("superadminuser"))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("User id superadminuser is not unique");
    }

    @Test
    void shouldResolveNonMatchingSingleUserFromBroadSearch() {
        KeycloakUser unrelatedUser = keycloakUser("unrelated-id", "salesuser-trainee");
        when(keycloakClient.searchUsers("salesuser", SEARCH_FIRST, SEARCH_MAX)).thenReturn(List.of(unrelatedUser));
        when(keycloakClient.getUserGroups("unrelated-id")).thenReturn(List.of(keycloakGroup("trainees")));

        List<String> groups = keycloakUserGroupManager.getUserGroups("salesuser");

        assertThat(groups).containsExactly("trainees");
    }

    @Test
    void shouldReturnNullWhenMembershipRepresentationsAreNull() {
        when(keycloakClient.searchUsers("salesuser", SEARCH_FIRST, SEARCH_MAX)).thenReturn(List.of(salesUser));
        when(keycloakClient.getUserGroups("sales-user-id")).thenReturn(null);

        List<String> groups = keycloakUserGroupManager.getUserGroups("salesuser");

        assertThat(groups).isNull();
    }

    @Test
    void shouldReturnNullWhenMembershipRepresentationsAreEmpty() {
        when(keycloakClient.searchUsers("salesuser", SEARCH_FIRST, SEARCH_MAX)).thenReturn(List.of(salesUser));
        when(keycloakClient.getUserGroups("sales-user-id")).thenReturn(List.of());

        List<String> groups = keycloakUserGroupManager.getUserGroups("salesuser");

        assertThat(groups).isNull();
    }

    private KeycloakUser keycloakUser(String id, String username) {
        KeycloakUser user = new KeycloakUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private KeycloakGroup keycloakGroup(String name) {
        KeycloakGroup group = new KeycloakGroup();
        group.setName(name);
        return group;
    }
}
