/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.common.security.keycloak.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.activiti.cloud.services.common.security.keycloak.KeycloakAccessTokenProvider;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.Optional;


public class KeycloakAccessTokenProviderTest {
    
    private KeycloakAccessTokenProvider subject = new KeycloakAccessTokenProvider() { };

    @Mock
    private KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal;

    @Mock
    private RefreshableKeycloakSecurityContext keycloakSecurityContext;
    
    @Mock
    private AccessToken accessToken;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testAccessToken() {
        // given
        when(principal.getKeycloakSecurityContext()).thenReturn(keycloakSecurityContext);
        when(keycloakSecurityContext.getToken()).thenReturn(accessToken);
        
        // when
        Optional<AccessToken> result = subject.accessToken(principal);
        
        // then
        assertThat(result).isPresent()
                          .contains(accessToken);
        
    }

    @Test
    public void testAccessTokenEmpty() {
        // given
        Principal principal = mock(UsernamePasswordAuthenticationToken.class);
        
        // when
        Optional<AccessToken> result = subject.accessToken(principal);
        
        // then
        assertThat(result).isEmpty();
    }
    
}
