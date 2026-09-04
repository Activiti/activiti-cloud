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
package org.activiti.cloud.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CustomAuthorizationManagerTest {

    @Test
    void should_prefixRolesPermissionsAndScopes() {
        CustomAuthorizationManager<Object> manager = new CustomAuthorizationManager<>(
            new String[] { "ROLE_1", "ROLE_2" },
            new String[] { "PERMISSION_1", "PERMISSION_2" },
            new String[] { "scope.1", "scope.2" }
        );

        assertThat(manager.getAuthoritiesWithAccess()).containsExactlyInAnyOrder(
            "ROLE_ROLE_1",
            "ROLE_ROLE_2",
            "PERMISSION_PERMISSION_1",
            "PERMISSION_PERMISSION_2",
            "SCOPE_scope.1",
            "SCOPE_scope.2"
        );
    }

    @Test
    void should_preserveTwoArgumentConstructor() {
        CustomAuthorizationManager<Object> manager = new CustomAuthorizationManager<>(
            new String[] { "ROLE_1" },
            new String[] { "PERMISSION_1" }
        );

        assertThat(manager.getAuthoritiesWithAccess()).containsExactlyInAnyOrder(
            "ROLE_ROLE_1",
            "PERMISSION_PERMISSION_1"
        );
    }

    @Test
    void should_grantAccessWhenScopeMatches() {
        CustomAuthorizationManager<Object> manager = managerWithAllAuthorityTypes();

        assertThat(manager.authorize(() -> authentication("SCOPE_scope.1"), new Object()).isGranted()).isTrue();
    }

    @Test
    void should_denyAccessWhenScopeIsMissingOrIncorrect() {
        CustomAuthorizationManager<Object> manager = new CustomAuthorizationManager<>(
            new String[] {},
            new String[] {},
            new String[] { "scope.1" }
        );

        assertThat(manager.authorize(() -> authentication(), new Object()).isGranted()).isFalse();
        assertThat(manager.authorize(() -> authentication("SCOPE_scope.2"), new Object()).isGranted()).isFalse();
    }

    @Test
    void should_grantAccessWhenRoleOrPermissionMatchesWithScopesConfigured() {
        CustomAuthorizationManager<Object> manager = managerWithAllAuthorityTypes();

        assertThat(manager.authorize(() -> authentication("ROLE_ROLE_1"), new Object()).isGranted()).isTrue();
        assertThat(
            manager.authorize(() -> authentication("PERMISSION_PERMISSION_1"), new Object()).isGranted()
        ).isTrue();
    }

    private CustomAuthorizationManager<Object> managerWithAllAuthorityTypes() {
        return new CustomAuthorizationManager<>(
            new String[] { "ROLE_1" },
            new String[] { "PERMISSION_1" },
            new String[] { "scope.1" }
        );
    }

    private TestingAuthenticationToken authentication(String... authorities) {
        return new TestingAuthenticationToken(
            "principal",
            "credentials",
            List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()
        );
    }
}
