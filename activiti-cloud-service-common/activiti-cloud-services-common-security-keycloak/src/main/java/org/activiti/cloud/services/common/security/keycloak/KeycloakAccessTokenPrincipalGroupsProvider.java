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

import java.security.Principal;
import java.util.List;
import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.cloud.services.common.security.keycloak.config.JwtAdapter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class KeycloakAccessTokenPrincipalGroupsProvider implements PrincipalGroupsProvider {

    private final JwtAccessTokenProvider keycloakAccessTokenProvider;
    private final KeycloakAccessTokenValidator keycloakAccessTokenValidator;

    public KeycloakAccessTokenPrincipalGroupsProvider(@NonNull JwtAccessTokenProvider keycloakAccessTokenProvider,
                                                      @NonNull KeycloakAccessTokenValidator keycloakAccessTokenValidator) {
        this.keycloakAccessTokenProvider = keycloakAccessTokenProvider;
        this.keycloakAccessTokenValidator = keycloakAccessTokenValidator;
    }

    @Override
    public List<String> getGroups(@NonNull Principal principal) {
        return keycloakAccessTokenProvider.accessToken(principal)
                                          //.filter(keycloakAccessTokenValidator::isValid)
                                          .map(JwtAdapter::getGroups)
                                          .orElseGet(this::empty);
    }

    protected @Nullable List<String> empty() {
        return null;
    }

}
