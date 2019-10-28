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

package org.activiti.cloud.services.common.security.keycloak.test.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.assertj.core.util.Sets;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Access;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Set;
import java.util.UUID;

public class WithMockKeycloakUserSecurityContextFactory implements WithSecurityContextFactory<WithMockKeycloakUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockKeycloakUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        Set<String> roles = Sets.newLinkedHashSet(annotation.roles());
        Set<String> groups = Sets.newLinkedHashSet(annotation.groups());

        RefreshableKeycloakSecurityContext securityContext = mock(RefreshableKeycloakSecurityContext.class);

        Access realmAccess = new Access();
        realmAccess.roles(roles);

        AccessToken accessToken = spy(new AccessToken());
        accessToken.setPreferredUsername(annotation.username());
        accessToken.setRealmAccess(realmAccess);
        accessToken.setOtherClaims("groups", groups);
        
        when(accessToken.isActive()).thenReturn(annotation.isActive());
        when(securityContext.getToken()).thenReturn(accessToken);
        
        KeycloakAccount account = new SimpleKeycloakAccount(new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(UUID.randomUUID().toString(),
                                                                                                                      securityContext),
                                                            roles,
                                                            securityContext);

        context.setAuthentication(new KeycloakAuthenticationToken(account, false, AuthorityUtils.createAuthorityList(annotation.roles())));
        
        return context;
    }

}
