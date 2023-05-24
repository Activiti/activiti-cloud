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
package org.activiti.cloud.services.common.security.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
public class KeycloakJwtAdapterTest {

    @Mock
    Jwt jwt;

    @InjectMocks
    KeycloakJwtAdapter keycloakJwtAdapter;

    @Test
    public void shouldReturnEmptyListWhenRealmAccessIsNull() {
        when(jwt.hasClaim("realm_access")).thenReturn(false);
        assertThat(keycloakJwtAdapter.getRoles()).isEmpty();
    }

    @Test
    public void shouldNotThrowAnyExceptionWhenRolesIsNull() {
        JSONObject rolesParent = new JSONObject();
        rolesParent.put("roles", null);
        when(jwt.hasClaim("realm_access")).thenReturn(true);
        when(jwt.getClaim("realm_access")).thenReturn(rolesParent);

        assertDoesNotThrow(() -> keycloakJwtAdapter.getRoles());
    }

    @Test
    public void shouldReturnRoles() {
        JSONObject rolesParent = new JSONObject();
        rolesParent.put("roles", List.of("roleA", "roleB"));
        when(jwt.hasClaim("realm_access")).thenReturn(true);
        when(jwt.getClaim("realm_access")).thenReturn(rolesParent);

        assertThat(keycloakJwtAdapter.getRoles()).hasSize(2).containsExactly("roleA", "roleB");
    }
}
