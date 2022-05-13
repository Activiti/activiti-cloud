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
package org.activiti.cloud.services.common.security.test.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
        String username = annotation.username();

        Map<String, Object> claims = prepareClaims(roles, groups, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put("testHeaderName", "testHeaderValue");

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        roles.forEach(role -> {
            grantedAuthorities.add(new SimpleGrantedAuthority(annotation.rolePrefix() + role));
        });

        String token = Jwts.builder()
            .setIssuer("Activiti Cloud")
            .setSubject(annotation.username())
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

    private Map<String, Object> prepareClaims(Set<String> roles, Set<String> groups, String username) {
        Map<String, Object> claims = new HashMap<>();

        JSONObject realm_access = new JSONObject();
        JSONArray rolesArray = new JSONArray();
        rolesArray.addAll(roles);
        realm_access.put("roles", rolesArray);

        JSONArray groupsArray = new JSONArray();
        groupsArray.addAll(groups);

        claims.put("realm_access", realm_access);
        claims.put("groups", groupsArray);
        claims.put("preferred_username", username);

        return claims;
    }

}
