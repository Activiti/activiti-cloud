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
package org.activiti.cloud.services.notifications.qraphql.ws.security;

import java.util.Collection;

import java.util.Set;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.GraphQLAccessTokenVerifier;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.GraphQLAccessToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.Attributes2GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAttributes2GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.User;

@Qualifier("websoket")
public class JWSAuthenticationManager implements AuthenticationManager {

    private final GraphQLAccessTokenVerifier tokenVerifier;
    private Attributes2GrantedAuthoritiesMapper authoritiesMapper = new SimpleAttributes2GrantedAuthoritiesMapper();

    public JWSAuthenticationManager(GraphQLAccessTokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        JWSAuthentication token = null;
        try {
            if(authentication instanceof JWSAuthentication) {

                token = JWSAuthentication.class.cast(authentication);

                String credentials = (String) token.getCredentials();

                GraphQLAccessToken accessToken = tokenVerifier.verifyToken(credentials);
                Set<String> roles = accessToken.getRoles();
                String preferredUsername = accessToken.getUsername();

                Collection<? extends GrantedAuthority> authorities = authoritiesMapper
                    .getGrantedAuthorities(roles);

                User user = new User(preferredUsername, credentials, authorities);

                token = new JWSAuthentication(credentials, user, authorities);
                token.setDetails(accessToken);
            }

        } catch (Exception e) {
            throw new BadCredentialsException("Invalid token", e);
        }

        return token;
    }

    public void setAuthoritiesMapper(Attributes2GrantedAuthoritiesMapper authoritiesMapper) {
        this.authoritiesMapper = authoritiesMapper;
    }

}
