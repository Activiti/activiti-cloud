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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class KeycloakClientPrincipalDetailsProviderTest {

    @Mock
    private KeycloakClient keycloakClient;

    @InjectMocks
    private KeycloakClientPrincipalDetailsProvider provider;

    @Test
    void should_lookUpGroupsByTheJwtSubjectClaim_when_thePrincipalIsJwtBacked() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("11111111-1111-1111-1111-111111111111");
        JwtAuthenticationToken principal = new JwtAuthenticationToken(jwt, List.of(), "testuser");
        KeycloakGroup group = new KeycloakGroup();
        group.setName("testgroup");
        when(keycloakClient.getUserGroups("11111111-1111-1111-1111-111111111111")).thenReturn(List.of(group));

        List<String> groups = provider.getGroups(principal);

        assertThat(groups).containsExactly("testgroup");
        verify(keycloakClient).getUserGroups("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void should_lookUpRolesByTheJwtSubjectClaim_when_thePrincipalIsJwtBacked() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("11111111-1111-1111-1111-111111111111");
        JwtAuthenticationToken principal = new JwtAuthenticationToken(jwt, List.of(), "testuser");
        KeycloakRoleMapping role = new KeycloakRoleMapping();
        role.setName("ACTIVITI_USER");
        when(keycloakClient.getUserRoleMapping("11111111-1111-1111-1111-111111111111")).thenReturn(List.of(role));

        List<String> roles = provider.getRoles(principal);

        assertThat(roles).containsExactly("ACTIVITI_USER");
        verify(keycloakClient).getUserRoleMapping("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void should_lookUpGroupsByThePrincipalName_when_thePrincipalIsNotJwtBacked() {
        Principal principal = () -> "testuser";
        KeycloakGroup group = new KeycloakGroup();
        group.setName("testgroup");
        when(keycloakClient.getUserGroups("testuser")).thenReturn(List.of(group));

        List<String> groups = provider.getGroups(principal);

        assertThat(groups).containsExactly("testgroup");
    }

    @Test
    void should_throwSecurityException_when_thePrincipalNameIsNull() {
        Principal principal = () -> null;

        assertThatThrownBy(() -> provider.getGroups(principal)).isInstanceOf(SecurityException.class);
    }
}
