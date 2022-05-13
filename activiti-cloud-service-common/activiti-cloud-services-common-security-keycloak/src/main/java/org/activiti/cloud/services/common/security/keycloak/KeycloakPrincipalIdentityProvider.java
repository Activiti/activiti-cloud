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
package org.activiti.cloud.services.common.security.keycloak;

import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.cloud.services.common.security.jwt.JwtAdapter;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenValidator;
import org.springframework.lang.NonNull;

import java.security.Principal;

public class KeycloakPrincipalIdentityProvider implements PrincipalIdentityProvider {

    private final JwtAccessTokenProvider keycloakAccessTokenProvider;
    private final JwtAccessTokenValidator jwtAccessTokenValidator;

    public KeycloakPrincipalIdentityProvider(@NonNull JwtAccessTokenProvider keycloakAccessTokenProvider,
                                             @NonNull JwtAccessTokenValidator jwtAccessTokenValidator) {
        this.keycloakAccessTokenProvider = keycloakAccessTokenProvider;
        this.jwtAccessTokenValidator = jwtAccessTokenValidator;
    }

    @Override
    public String getUserId(@NonNull Principal principal) {
        return keycloakAccessTokenProvider.accessToken(principal)
                                          .filter(jwtAccessTokenValidator::isValid)
                                          .map(JwtAdapter::getUserName)
                                          .orElseThrow(this::securityException);
    }

    protected SecurityException securityException() {
        return new SecurityException("Invalid accessToken object instance");
    }
}
