/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class KeycloakClientPrincipalDetailsProvider implements PrincipalGroupsProvider, PrincipalRolesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakClientPrincipalDetailsProvider.class);

    private final KeycloakClient keycloakClient;

    public KeycloakClientPrincipalDetailsProvider(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @Override
    public List<String> getGroups(Principal principal) {
        String id = subjectId(principal);
        List<String> groups = keycloakClient
            .getUserGroups(id)
            .stream()
            .map(KeycloakGroup::getName)
            .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        LOGGER.debug("Resolved {} group(s) for Keycloak user {}", groups.size(), id);
        return groups;
    }

    @Override
    public List<String> getRoles(Principal principal) {
        String id = subjectId(principal);
        List<String> roles = keycloakClient
            .getUserRoleMapping(id)
            .stream()
            .map(KeycloakRoleMapping::getName)
            .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        LOGGER.debug("Resolved {} role(s) for Keycloak user {}", roles.size(), id);
        return roles;
    }

    /**
     * The Keycloak Admin API identifies users by their internal id, not their username - so for a
     * JWT-backed principal this must come from the {@code sub} claim. {@code principal.getName()}
     * is not reliable here: this codebase's own {@code JwtAuthenticationToken} construction sets
     * the principal name to the username, not the subject.
     */
    protected String subjectId(Principal principal) {
        if (principal instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String subject = jwtAuthenticationToken.getToken().getSubject();
            LOGGER.debug("Resolved Keycloak subject id {} from the JWT 'sub' claim", subject);
            return subject;
        }
        String name = Optional.of(principal).map(Principal::getName).orElseThrow(this::securityException);
        LOGGER.debug(
            "Principal is not JWT-backed; falling back to principal.getName() ({}) as the Keycloak subject id",
            name
        );
        return name;
    }

    protected SecurityException securityException() {
        return new SecurityException("Invalid Keycloak principal subject id");
    }
}
