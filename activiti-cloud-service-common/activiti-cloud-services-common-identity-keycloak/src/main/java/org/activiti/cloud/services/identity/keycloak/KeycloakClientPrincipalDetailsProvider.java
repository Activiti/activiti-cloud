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

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;

public class KeycloakClientPrincipalDetailsProvider implements PrincipalGroupsProvider, PrincipalRolesProvider {

    private final KeycloakClient keycloakClient;

    public KeycloakClientPrincipalDetailsProvider(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @Override
    public List<String> getGroups(Principal principal) {
        return keycloakClient
            .getUserGroups(subjectId(principal))
            .stream()
            .map(KeycloakGroup::getName)
            .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public List<String> getRoles(Principal principal) {
        return keycloakClient
            .getUserRoleMapping(subjectId(principal))
            .stream()
            .map(KeycloakRoleMapping::getName)
            .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    protected String subjectId(Principal principal) {
        return Optional.of(principal).map(Principal::getName).orElseThrow(this::securityException);
    }

    protected SecurityException securityException() {
        return new SecurityException("Invalid Keycloak principal subject id");
    }
}
