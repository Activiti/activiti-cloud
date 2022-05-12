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
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.cloud.services.common.security.keycloak.config.JwtAdapter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class JtwAccessTokenPrincipalRolesProvider implements PrincipalRolesProvider {

    private final JwtAccessTokenProvider jwtAccessTokenProvider;
    private final JwtAccessTokenValidator jwtAccessTokenValidator;

    public JtwAccessTokenPrincipalRolesProvider(@NonNull JwtAccessTokenProvider keycloakSecurityContextProvider,
                                                     @NonNull JwtAccessTokenValidator jwtAccessTokenValidator) {
        this.jwtAccessTokenProvider = keycloakSecurityContextProvider;
        this.jwtAccessTokenValidator = jwtAccessTokenValidator;
    }

    @Override
    public List<String> getRoles(@NonNull Principal principal) {
        return jwtAccessTokenProvider.accessToken(principal)
                                          .filter(jwtAccessTokenValidator::isValid)
                                          .map(JwtAdapter::getRoles)
                                          .orElseGet(this::empty);
    }

    protected @Nullable List<String> empty() {
        return null;
    }

}
