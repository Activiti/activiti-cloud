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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.activiti.cloud.identity.model.Role;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakUserToUserTest {

  @Mock
  private KeycloakClient keycloakClient;

  @Mock
  private KeycloakRoleMappingToRole keycloakRoleMappingToRole;

  @InjectMocks
  private KeycloakUserToUser keycloakUserToUser;

  @Test
  void shouldTransformKeycloakUserToUser() {

    KeycloakUser kUser = new KeycloakUser();
    kUser.setId("123");
    kUser.setEmail("test@test.it");
    kUser.setFirstName("Ping");
    kUser.setLastName("Pong");
    kUser.setUsername("pingPong");

    KeycloakRoleMapping keycloakRoleMapping = new KeycloakRoleMapping();
    List<KeycloakRoleMapping> keycloakRoleMappingList = List.of(keycloakRoleMapping);
    when(keycloakClient.getUserRoleMapping(eq("123"))).thenReturn(keycloakRoleMappingList);

    Role role = new Role();
    role.setId("456");
    role.setName("ROLE");
    List<Role> rolesList = List.of(role);
    when(keycloakRoleMappingToRole.toRoles(eq(keycloakRoleMappingList))).thenReturn(rolesList);

    User user = keycloakUserToUser.toUser(kUser);

    assertThat(user.getId()).isEqualTo(kUser.getId());
    assertThat(user.getDisplayName()).isEqualTo("Ping Pong");
    assertThat(user.getEmail()).isEqualTo(kUser.getEmail());
    assertThat(user.getUsername()).isEqualTo(kUser.getUsername());
    assertThat(user.getRoles()).isEqualTo(rolesList);
  }
}
