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

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.lang.NonNull;

import java.security.Principal;
import java.util.Optional;

public interface KeycloakAccessTokenProvider {

    default Optional<AccessToken> accessToken(@NonNull Principal principal) {
        return Optional.of(principal)
                       .filter(KeycloakPrincipal.class::isInstance)
                       .map(KeycloakPrincipal.class::cast)
                       .map(KeycloakPrincipal::getKeycloakSecurityContext)
                       .map(KeycloakSecurityContext::getToken);
    }

}
