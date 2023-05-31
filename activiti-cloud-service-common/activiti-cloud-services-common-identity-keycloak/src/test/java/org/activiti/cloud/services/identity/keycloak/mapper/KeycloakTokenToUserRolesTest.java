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

import static org.activiti.cloud.services.identity.keycloak.mapper.KeycloakTokenToUserRoles.REALM;
import static org.activiti.cloud.services.identity.keycloak.mapper.KeycloakTokenToUserRoles.RESOURCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.nimbusds.jose.util.JSONObjectUtils;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.identity.model.UserApplicationAccess;
import org.activiti.cloud.identity.model.UserRoles;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakTokenToUserRolesTest {

    private Jwt jwt;

    @Test
    public void shouldTransformJwtTokenToUserRoles() {
        mockJwt(true, true);

        UserRoles userRoles = KeycloakTokenToUserRoles.toUserRoles(jwt);

        assertThat(userRoles.getGlobalAccess().getRoles()).hasSize(3).containsOnly("role1", "role2", "role3");
        assertThat(userRoles.getApplicationAccess())
            .extracting(UserApplicationAccess::getName, UserApplicationAccess::getRoles)
            .containsOnly(tuple("resource1", List.of("role1")), tuple("resource2", List.of("role1", "role2")));
    }

    @Test
    public void shouldReturnEmptyUserRolesWhenTokenIsNull() {
        UserRoles userRoles = assertDoesNotThrow(() -> KeycloakTokenToUserRoles.toUserRoles(null));
        assertThat(userRoles).isNotNull();
    }

    @Test
    public void shouldReturnGlobalAccessWhenResourceAccessIsNull() {
        mockJwt(false, true);

        UserRoles userRoles = KeycloakTokenToUserRoles.toUserRoles(jwt);
        assertThat(userRoles.getGlobalAccess().getRoles()).hasSize(3).containsOnly("role1", "role2", "role3");
        assertThat(userRoles.getApplicationAccess()).isEmpty();
    }

    @Test
    public void shouldReturnApplicationAccessWhenRealmAccessIsNull() {
        mockJwt(true, false);

        UserRoles userRoles = KeycloakTokenToUserRoles.toUserRoles(jwt);
        assertThat(userRoles.getApplicationAccess())
            .extracting(UserApplicationAccess::getName, UserApplicationAccess::getRoles)
            .containsOnly(tuple("resource1", List.of("role1")), tuple("resource2", List.of("role1", "role2")));

        assertThat(userRoles.getGlobalAccess().getRoles()).isEmpty();
    }

    private void mockJwt(boolean withResourceRoleMappings, boolean withRealmRoleMappings) {
        Map<String, Object> resourceRoleMappings;
        Map<String, Object> realmRoleMappings;

        if (withResourceRoleMappings) {
            resourceRoleMappings = JSONObjectUtils.newJSONObject();
            Map<String, Object> resource1Roles = JSONObjectUtils.newJSONObject();
            resource1Roles.put("roles", List.of("role1"));
            Map<String, Object> resource2Roles = JSONObjectUtils.newJSONObject();
            resource2Roles.put("roles", List.of("role1", "role2"));
            resourceRoleMappings.put("resource1", resource1Roles);
            resourceRoleMappings.put("resource2", resource2Roles);
        } else {
            resourceRoleMappings = null;
        }

        if (withRealmRoleMappings) {
            realmRoleMappings = JSONObjectUtils.newJSONObject();
            realmRoleMappings.putAll(Map.of("roles", List.of("role1", "role2", "role3")));
        } else {
            realmRoleMappings = null;
        }

        jwt =
            Jwt
                .withTokenValue("mock-token-value")
                .header("mock-header", "mock-header-value")
                .claims(e -> {
                    if (withRealmRoleMappings) {
                        e.put(REALM, realmRoleMappings);
                    }
                    if (withResourceRoleMappings) {
                        e.put(RESOURCE, resourceRoleMappings);
                    }
                })
                .build();
    }
}
