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

import java.security.Principal;
import java.util.Optional;
import org.activiti.cloud.services.common.security.keycloak.KeycloakSecurityContextPrincipalProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
public class KeycloakSecurityContextPrincipalProviderTest {

    private final KeycloakSecurityContextPrincipalProvider subject = new KeycloakSecurityContextPrincipalProvider();

    @Mock
    private Jwt jwt;

    @Test
    public void should_getCurrentPrincipal() {
        // given
        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);

        // when
        Optional<Principal> result = subject.getCurrentPrincipal();

        // then
        assertThat(result).isPresent()
                          .containsInstanceOf(JwtAuthenticationToken.class)
                          .contains((jwtAuthenticationToken));
    }

    @Test
    public void should_getEmptyCurrentPrincipal() {
        // given
        SecurityContextHolder.clearContext();

        // when
        Optional<Principal> result = subject.getCurrentPrincipal();

        // then
        assertThat(result).isEmpty();
    }

}
