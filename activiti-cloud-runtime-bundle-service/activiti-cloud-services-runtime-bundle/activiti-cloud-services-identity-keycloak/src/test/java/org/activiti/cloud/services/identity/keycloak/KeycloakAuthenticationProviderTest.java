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

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.security.config.authentication.AuthenticationManagerBeanDefinitionParser.NullAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class KeycloakAuthenticationProviderTest {

    //TODO replace null implementation
    private KeycloakActivitiAuthenticationProvider keycloakActivitiAuthenticationProvider = spy(new KeycloakActivitiAuthenticationProvider(new JwtDecoder() {
        @Override
        public Jwt decode(String token) throws JwtException {
            return null;
        }
    }));
    private KeycloakAuthenticationToken token;
    private RefreshableKeycloakSecurityContext keycloakSecurityContext;

    @BeforeEach
    public void setUp() {
        keycloakSecurityContext = mock(RefreshableKeycloakSecurityContext.class);
        KeycloakPrincipal principal = new KeycloakPrincipal("bob",
                                                            keycloakSecurityContext);
        KeycloakAccount keycloakAccount = new SimpleKeycloakAccount(principal,
                                                                    new HashSet<>(Arrays.asList("role1",
                                                                                                "role2")),
                                                                    keycloakSecurityContext);
        token = new KeycloakAuthenticationToken(keycloakAccount,
                                                false);
    }

    @Test
    public void authenticateShouldUseNameFromAuthenticationWhenPreferredUserNameIsNotSet() {

        //when
        Authentication authentication = keycloakActivitiAuthenticationProvider.authenticate(token);

        //then
        assertThat(authentication).isNotNull();
        verify(keycloakActivitiAuthenticationProvider).setAuthenticatedUserId("bob");
    }

    @Test
    public void authenticateShouldUsePreferredUsernameWhenSet() {

        //given
        AccessToken accessToken = mock(AccessToken.class);
        when(keycloakSecurityContext.getToken()).thenReturn(accessToken);
        when(accessToken.getPreferredUsername()).thenReturn("bob@any.org");

        //when
        Authentication authentication = keycloakActivitiAuthenticationProvider.authenticate(token);

        //then
        assertThat(authentication).isNotNull();
        verify(keycloakActivitiAuthenticationProvider).setAuthenticatedUserId("bob@any.org");
    }
}
