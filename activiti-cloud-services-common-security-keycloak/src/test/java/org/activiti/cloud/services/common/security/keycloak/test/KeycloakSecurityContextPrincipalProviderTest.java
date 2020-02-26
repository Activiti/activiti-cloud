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

import org.activiti.cloud.services.common.security.keycloak.KeycloakSecurityContextPrincipalProvider;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;


public class KeycloakSecurityContextPrincipalProviderTest {

    private KeycloakSecurityContextPrincipalProvider subject = new KeycloakSecurityContextPrincipalProvider();
    
    @Test
    public void testGetCurrentPrincipal() {
        // given
        String subjectId = UUID.randomUUID().toString();
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<>(subjectId,
                                                                                                  new RefreshableKeycloakSecurityContext());
        KeycloakAccount account = new SimpleKeycloakAccount(principal,
                                                            Collections.emptySet(),
                                                            principal.getKeycloakSecurityContext());

        SecurityContextHolder.getContext()
                             .setAuthentication(new KeycloakAuthenticationToken(account,
                                                                                false));
                
        // when
        Optional<Principal> result = subject.getCurrentPrincipal();
        
        // then
        assertThat(result).isPresent()
                          .containsInstanceOf(KeycloakPrincipal.class)
                          .contains(principal);
        
    }
    
    @Test
    public void testGetCurrentPrincipalEmpty() {
        // given
        SecurityContextHolder.clearContext();

        // when
        Optional<Principal> result = subject.getCurrentPrincipal();
        
        // then
        assertThat(result).isEmpty();
    }

}
