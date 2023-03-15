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
package org.activiti.cloud.services.identity.keycloak.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.activiti.cloud.identity.model.Role;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.junit.jupiter.api.Test;

class KeycloakRoleMappingToRoleTest {

    private KeycloakRoleMappingToRole keycloakRoleMappingToRole = new KeycloakRoleMappingToRole();

    @Test
    public void shouldTransformKeycloakRoleMappingToRole() {
        KeycloakRoleMapping kRole = new KeycloakRoleMapping();
        kRole.setId("123");
        kRole.setName("test");

        Role role = keycloakRoleMappingToRole.toRole(kRole);

        assertThat(role.getId()).isEqualTo(kRole.getId());
        assertThat(role.getName()).isEqualTo(kRole.getName());
    }

    @Test
    public void shouldTransformKeycloakRoleMappingsToRoles() {
        KeycloakRoleMapping kRole1 = new KeycloakRoleMapping();
        kRole1.setId("123");
        kRole1.setName("test");

        KeycloakRoleMapping kRole2 = new KeycloakRoleMapping();
        kRole2.setId("456");
        kRole2.setName("test 2");

        List<Role> role = keycloakRoleMappingToRole.toRoles(List.of(kRole1, kRole2));

        assertThat(role.get(0).getId()).isEqualTo(kRole1.getId());
        assertThat(role.get(0).getName()).isEqualTo(kRole1.getName());
        assertThat(role.get(1).getId()).isEqualTo(kRole2.getId());
        assertThat(role.get(1).getName()).isEqualTo(kRole2.getName());
    }
}
