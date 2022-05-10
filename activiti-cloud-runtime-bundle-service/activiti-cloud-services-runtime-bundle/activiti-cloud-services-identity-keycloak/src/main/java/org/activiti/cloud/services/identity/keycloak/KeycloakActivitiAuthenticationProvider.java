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
package org.activiti.cloud.services.identity.keycloak;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

public class KeycloakActivitiAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider authenticationProvider;

    public KeycloakActivitiAuthenticationProvider(JwtDecoder jwtDecoder) {
        this.authenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        Authentication result = authenticationProvider.authenticate(authentication);

        String userId = result.getName(); //this will be keycloak id

        Object principal = result.getPrincipal();
        if (principal instanceof Jwt) {
            userId = ((Jwt) principal).getClaims().get("preferred_username").toString();
        }

        setAuthenticatedUserId(userId);
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authenticationProvider.supports(authentication);
    }

    public void setAuthenticatedUserId(String userId) {
        org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(userId);
    }
}
