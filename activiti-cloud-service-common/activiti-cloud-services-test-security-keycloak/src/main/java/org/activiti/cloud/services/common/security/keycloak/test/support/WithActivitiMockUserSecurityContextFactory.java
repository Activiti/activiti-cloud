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

import static com.nimbusds.oauth2.sdk.token.TokenTypeURI.ACCESS_TOKEN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import io.jsonwebtoken.impl.TextCodec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mockito.internal.util.collections.Sets;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithActivitiMockUserSecurityContextFactory implements WithSecurityContextFactory<WithActivitiMockUser> {

    @Override
    public SecurityContext createSecurityContext(WithActivitiMockUser annotation) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        Set<String> roles = Sets.newSet(annotation.roles());
        Set<String> groups = Sets.newSet(annotation.groups());

//        RefreshableKeycloakSecurityContext securityContext = mock(RefreshableKeycloakSecurityContext.class);
//        when(securityContext.isActive()).thenReturn(true);
//
//        Access realmAccess = new Access();
//        realmAccess.roles(roles);
//
//        AccessToken accessToken = spy(new AccessToken());
//        accessToken.setPreferredUsername(annotation.username());
//        accessToken.setRealmAccess(realmAccess);
//        accessToken.setOtherClaims("groups", groups);
//
//        when(accessToken.isActive()).thenReturn(annotation.isActive());
//        when(securityContext.getToken()).thenReturn(accessToken);
//
//        KeycloakAccount account = new SimpleKeycloakAccount(new KeycloakPrincipal<>(UUID.randomUUID().toString(), securityContext),
//            roles,
//            securityContext);
//
//        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
//
//        for (String role : account.getRoles()) {
//            grantedAuthorities.add(new KeycloakRole(role));
//        }
//
//        SimpleAuthorityMapper grantedAuthoritiesMapper = new SimpleAuthorityMapper();
//        grantedAuthoritiesMapper.setPrefix(annotation.rolePrefix());
//
//        context.setAuthentication(new KeycloakAuthenticationToken(account,
//            annotation.isInteractive(),
//            grantedAuthoritiesMapper.mapAuthorities(grantedAuthorities)));

        Map<String, Object> claims = new HashMap<>();
        claims.put("groups", groups);
//        claims.put("sub", annotation.username());

        Map<String, Object> headers = new HashMap<>();
        headers.put("test", "test");


        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        roles.forEach(role -> {
            grantedAuthorities.add(new SimpleGrantedAuthority(annotation.rolePrefix() + role));
        });

        String token = Jwts.builder()
            .setIssuer("Activiti Cloud")
            .setSubject(annotation.username())
            .claim("groups", groups)
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(
                SignatureAlgorithm.HS256,
                TextCodec.BASE64.decode("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=")
            )
            .compact();

        Jwt jwt = new Jwt(token,
            Instant.now(),
            Instant.now().plusSeconds(600),
            headers,
            claims);

        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt,
            grantedAuthorities);

        securityContext.setAuthentication(jwtAuthenticationToken);

        return securityContext;
    }

}
