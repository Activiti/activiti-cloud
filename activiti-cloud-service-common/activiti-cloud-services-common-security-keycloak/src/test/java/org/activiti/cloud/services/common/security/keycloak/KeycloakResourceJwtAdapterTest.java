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

import com.nimbusds.jose.shaded.json.JSONObject;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class KeycloakResourceJwtAdapterTest {

    @Mock
    Jwt jwt;

    @InjectMocks
    KeycloakResourceJwtAdapter keycloakResourceJwtAdapter;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(keycloakResourceJwtAdapter, "resourceId",
            "app");
    }

    @Test
    public void shouldReturnEmptyListWhenResourceAccessIsNull() {
        when(jwt.hasClaim("resource_access")).thenReturn(false);
        assertThat(keycloakResourceJwtAdapter.getRoles()).isEmpty();
    }

    @Test
    public void shouldNotThrowAnyExceptionWhenRolesIsNull(){
        JSONObject rolesParent = new JSONObject();
        rolesParent.put("roles", null);
        when(jwt.hasClaim("resource_access")).thenReturn(true);
        when(jwt.getClaim("resource_access")).thenReturn(rolesParent);

        assertDoesNotThrow(() -> keycloakResourceJwtAdapter.getRoles());
    }

    @Test
    public void shouldReturnRoles(){
        JSONObject client = new JSONObject();
        JSONObject roles = new JSONObject();
        roles.put("roles", List.of("roleA", "roleB"));
        client.put("app", roles);
        when(jwt.hasClaim("resource_access")).thenReturn(true);
        when(jwt.getClaim("resource_access")).thenReturn(client);

        assertThat(keycloakResourceJwtAdapter.getRoles())
            .hasSize(2)
            .containsExactly("roleA", "roleB");
    }

}
