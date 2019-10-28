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

package org.activiti.cloud.services.common.security.keycloak;

import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.keycloak.representations.AccessToken;
import org.springframework.lang.NonNull;

import java.security.Principal;
import java.util.Optional;

public class KeycloakPrincipalIdentityProvider implements PrincipalIdentityProvider {
    
    private final KeycloakAccessTokenProvider keycloakAccessTokenProvider;
    private final KeycloakAccessTokenValidator keycloakAccessTokenValidator; 
    
    public KeycloakPrincipalIdentityProvider(@NonNull KeycloakAccessTokenProvider keycloakAccessTokenProvider,
                                             @NonNull KeycloakAccessTokenValidator keycloakAccessTokenValidator) {
        this.keycloakAccessTokenProvider = keycloakAccessTokenProvider;
        this.keycloakAccessTokenValidator = keycloakAccessTokenValidator;
    }

    @Override
    public String getUserId(@NonNull Principal principal) {
        return keycloakAccessTokenProvider.accessToken(principal)
                                          .filter(keycloakAccessTokenValidator::isValid)
                                          .map(this::getUserId)
                                          .orElseThrow(this::securityException);
    }
    
    protected String getUserId(AccessToken accessToken) {
        return Optional.ofNullable(accessToken)
                       .map(AccessToken::getPreferredUsername)
                       .orElseThrow(this::securityException);
    }
    
    protected SecurityException securityException() {
        return new SecurityException("Invalid accessToken object instance");
    }    
}
