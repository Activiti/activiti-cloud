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
package org.activiti.cloud.services.identity.keycloak.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Map;
import java.util.Set;
import org.activiti.cloud.identity.model.UserApplicationAccess;
import org.activiti.cloud.identity.model.UserRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Access;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class KeycloakTokenToUserRolesTest {

    @InjectMocks
    private KeycloakTokenToUserRoles keycloakTokenToUserRoles;

    private AccessToken accessToken = new AccessToken();

    @BeforeEach
    public void setupData() {
        Access realmAccess = new Access();
        realmAccess.roles(Set.of("role1", "role2", "role3"));
        accessToken.setRealmAccess(realmAccess);

        Map<String, Access> resourceAccess = Map.of(
            "resource1", new Access().roles(Set.of("role1")),
            "resource2", new Access().roles(Set.of("role1", "role2")));
        accessToken.setResourceAccess(resourceAccess);
    }

    @Test
    public void shouldTransformKeycloakTokenToUserRoles() {
        UserRoles userRoles = keycloakTokenToUserRoles.toUserRoles(accessToken);

        assertThat(userRoles.getGlobalAccess().getRoles())
            .hasSize(3)
            .containsOnly("role1", "role2", "role3");
        assertThat(userRoles.getApplicationAccess())
            .extracting(UserApplicationAccess::getName, UserApplicationAccess::getRoles)
            .containsOnly(
                tuple("resource1", Set.of("role1")),
                tuple("resource2", Set.of("role1", "role2")));
    }

    @Test
    public void shouldReturnEmptyUserRolesWhenTokenIsNull() {
        UserRoles userRoles = assertDoesNotThrow(() -> keycloakTokenToUserRoles.toUserRoles(null));
        assertThat(userRoles).isNotNull();
    }

    @Test
    public void shouldReturnGlobalAccessWhenResourceAccessIsNull() {
        accessToken.setResourceAccess(null);
        UserRoles userRoles = keycloakTokenToUserRoles.toUserRoles(accessToken);
        assertThat(userRoles.getGlobalAccess().getRoles())
            .hasSize(3)
            .containsOnly("role1", "role2", "role3");
        assertThat(userRoles.getApplicationAccess())
            .isEmpty();
    }

    @Test
    public void shouldReturnApplicationAccessWhenRealmAccessIsNull() {
        accessToken.setRealmAccess(null);
        UserRoles userRoles = keycloakTokenToUserRoles.toUserRoles(accessToken);
        assertThat(userRoles.getApplicationAccess())
            .extracting(UserApplicationAccess::getName, UserApplicationAccess::getRoles)
            .containsOnly(
                tuple("resource1", Set.of("role1")),
                tuple("resource2", Set.of("role1", "role2")));

        assertThat(userRoles.getGlobalAccess().getRoles())
            .isEmpty();
    }

}
