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

import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.springframework.lang.NonNull;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KeycloakPrincipalGroupsProviderChain implements PrincipalGroupsProvider {
    
    private final List<PrincipalGroupsProvider> providers;
    
    public KeycloakPrincipalGroupsProviderChain(@NonNull List<PrincipalGroupsProvider> providers) {
        this.providers = Collections.unmodifiableList(providers);
    }

    @Override
    public List<String> getGroups(@NonNull Principal principal) {
        return providers.stream()
                        .map(provider -> provider.getGroups(principal))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElseThrow(this::securityException);
    }

    protected SecurityException securityException() {
        return new SecurityException("Invalid principal security access token groups");
    }
}
