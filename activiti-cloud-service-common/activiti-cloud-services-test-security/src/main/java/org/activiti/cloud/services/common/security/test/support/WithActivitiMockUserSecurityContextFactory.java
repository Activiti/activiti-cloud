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

import com.nimbusds.jose.util.JSONArrayUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.activiti.cloud.services.common.security.test.support.WithActivitiMockUser.ResourceRoles;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithActivitiMockUserSecurityContextFactory implements WithSecurityContextFactory<WithActivitiMockUser> {

    @Autowired
    private RolesClaimProvider rolesClaimProvider;

    @Override
    public SecurityContext createSecurityContext(WithActivitiMockUser annotation) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        Set<String> globalRoles = Sets.newSet(annotation.roles());
        Set<String> groups = Sets.newSet(annotation.groups());
        String username = annotation.username();
        Map<String, String[]> resourceRoles = Arrays
            .stream(annotation.resourcesRoles())
            .collect(Collectors.toMap(ResourceRoles::resource, ResourceRoles::roles));

        Map<String, Object> claims = prepareClaims(globalRoles, groups, username, resourceRoles);

        Map<String, Object> headers = new HashMap<>();
        headers.put("testHeaderName", "testHeaderValue");

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        globalRoles.forEach(role -> {
            grantedAuthorities.add(new SimpleGrantedAuthority(annotation.rolePrefix() + role));
        });

        String token = Jwts
            .builder()
            .setIssuer("Activiti Cloud")
            .setSubject(annotation.username())
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.decode("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E="))
            .compact();

        Jwt jwt = new Jwt(token, Instant.now(), Instant.now().plusSeconds(600), headers, claims);

        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt, grantedAuthorities);

        securityContext.setAuthentication(jwtAuthenticationToken);

        return securityContext;
    }

    private Map<String, Object> prepareClaims(
        Set<String> globalRoles,
        Set<String> groups,
        String username,
        Map<String, String[]> resourceRoles
    ) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("preferred_username", username);

        rolesClaimProvider.setGlobalRoles(globalRoles, claims);
        rolesClaimProvider.setResourceRoles(resourceRoles, claims);

        List<Object> groupsArray = JSONArrayUtils.newJSONArray();
        groupsArray.addAll(groups);
        claims.put("groups", groupsArray);

        return claims;
    }
}
