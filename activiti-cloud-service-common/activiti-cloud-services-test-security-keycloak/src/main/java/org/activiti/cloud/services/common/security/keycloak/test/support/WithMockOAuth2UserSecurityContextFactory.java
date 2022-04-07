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
package org.activiti.cloud.services.common.security.keycloak.test.support;

import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mockito.internal.util.collections.Sets;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockOAuth2UserSecurityContextFactory implements WithSecurityContextFactory<WithMockOAuth2User> {

    @Override
    public SecurityContext createSecurityContext(WithMockOAuth2User annotation) {

        SecurityContext mainContext = SecurityContextHolder.createEmptyContext(); //DONE

        Set<String> roles = Sets.newSet(annotation.roles()); //DONE
        //roles in keycloak are OidcUserAuthority in oidc
        Set<String> groups = Sets.newSet(annotation.groups()); //DONE
        //groups are going to be treated as claims in oidc


        //-------------------------------------

//        RefreshableKeycloakSecurityContext securityContext = mock(RefreshableKeycloakSecurityContext.class);
//        when(securityContext.isActive()).thenReturn(true);

//        Access realmAccess = new Access();
//        realmAccess.roles(roles);

//        AccessToken accessToken = spy(new AccessToken());
//        accessToken.setPreferredUsername(annotation.username()); // DONE
//        accessToken.setRealmAccess(realmAccess); //role setting DONE
//        accessToken.setOtherClaims("groups", groups); // DONE

//        when(accessToken.isActive()).thenReturn(annotation.isActive());
//        when(securityContext.getToken()).thenReturn(accessToken);

//        KeycloakAccount account = new SimpleKeycloakAccount(new KeycloakPrincipal<>(UUID.randomUUID().toString(), securityContext),
//            roles,
//            securityContext);



//        grantedAuthoritiesMapper.setPrefix(annotation.rolePrefix());

//        mainContext.setAuthentication(new KeycloakAuthenticationToken(account,
//            annotation.isInteractive(),
//            grantedAuthoritiesMapper.mapAuthorities(grantedAuthorities)));

        //-------------------------------------
        //TOP-DOWN APPROACH


//        new SimpleGrantedAuthority(AuthoritiesConstants.USER);

        Map<String, Object> claims = new HashMap<>();
        claims.put("groups", groups);
//        claims.put(annotation.username(), groups);
        claims.put("sub", annotation.username());

        OidcIdToken idToken = new OidcIdToken(ID_TOKEN,
            Instant.now(),
            Instant.now().plusSeconds(60),
            claims);

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        roles.forEach(role -> {
            grantedAuthorities.add(new SimpleGrantedAuthority(role));
        });

        OidcUser principal = new DefaultOidcUser(grantedAuthorities, idToken);

//        OAuth2User principal = new DefaultOAuth2User(grantedAuthorities,
//            claims,
//            annotation.username());

        SimpleAuthorityMapper grantedAuthoritiesMapper = new SimpleAuthorityMapper();

        OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(principal,
//            grantedAuthoritiesMapper.mapAuthorities(grantedAuthorities),
            grantedAuthorities,
            "oidc");

        mainContext.setAuthentication(oAuth2AuthenticationToken);

        //-------------------------------------

        return mainContext;
    }

}
