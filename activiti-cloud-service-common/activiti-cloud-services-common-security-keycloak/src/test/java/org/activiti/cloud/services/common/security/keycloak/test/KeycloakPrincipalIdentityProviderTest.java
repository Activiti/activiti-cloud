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
package org.activiti.cloud.services.common.security.keycloak.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.activiti.cloud.services.common.security.keycloak.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakAccessTokenValidator;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalIdentityProvider;
import org.activiti.cloud.services.common.security.keycloak.config.JwtAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
public class KeycloakPrincipalIdentityProviderTest {

    @InjectMocks
    private KeycloakPrincipalIdentityProvider principalIdentityProvider;

    @Mock
    private KeycloakAccessTokenValidator keycloakAccessTokenValidator;

    @Mock
    private JwtAdapter jwtAdapter;

    @Mock
    private JwtAccessTokenProvider jwtAccessTokenProvider;

    @Mock
    private JwtAuthenticationToken principal;

    @Test
    public void should_retrieveUserId_when_tokenIsValid() {
        // given
        String USERNAME = "usename";
        when(jwtAdapter.getUserName()).thenReturn(USERNAME);
        when(jwtAccessTokenProvider.accessToken(principal)).thenReturn(Optional.of(jwtAdapter));
        when(keycloakAccessTokenValidator.isValid(jwtAdapter)).thenReturn(true);

        // when
        String userId = principalIdentityProvider.getUserId(principal);

        // then
        assertThat(userId).isEqualTo(USERNAME);
    }

    @Test
    public void should_throwSecurityException_when_tokenIsNotValid() {
        // given
        when(jwtAccessTokenProvider.accessToken(principal)).thenReturn(Optional.of(jwtAdapter));
        when(keycloakAccessTokenValidator.isValid(jwtAdapter)).thenReturn(false);

        // when
        Throwable thrown = catchThrowable(() -> {
            principalIdentityProvider.getUserId(principal);
        });

        // then
        assertThat(thrown).isInstanceOf(SecurityException.class);
    }

}
