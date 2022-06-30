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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakClientRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakUser;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = {KeycloakClientApplication.class},
    properties = {
        "keycloak.realm=activiti",
        "keycloak.use-resource-role-mappings=false"
    }
)
@ContextConfiguration(initializers = {KeycloakContainerApplicationInitializer.class})
public class KeycloakClientIT {

    @Autowired
    private KeycloakClient keycloakClient;

    @Test
    public void should_searchUsers() {
        List<KeycloakUser> users = keycloakClient.searchUsers("hr", 0, 50);

        assertThat(users).hasSize(2);
        assertThat(users).extracting("username").contains("hruser");
        assertThat(users).extracting("username").contains("hradmin");
    }

    @Test
    public void should_searchUsers_when_paginated() {
        List<KeycloakUser> usersPage1 = keycloakClient.searchUsers("hr", 0, 1);

        assertThat(usersPage1).hasSize(1);
        assertThat(usersPage1).extracting("username").contains("hradmin");

        List<KeycloakUser> usersPage2 = keycloakClient.searchUsers("hr", 1, 1);

        assertThat(usersPage2).hasSize(1);
        assertThat(usersPage2).extracting("username").contains("hruser");
    }

    @Test
    public void should_getUserGroups() {
        List<KeycloakUser> users = keycloakClient.searchUsers("hruser", 0, 50);
        List<KeycloakGroup> groups = keycloakClient.getUserGroups(users.get(0).getId());

        assertThat(users).hasSize(1);
        assertThat(groups).extracting("name").contains("hr");
    }

    @Test
    public void should_searchGroups() {
        List<KeycloakGroup> groups = keycloakClient.searchGroups("testgroup", 0, 50);

        assertThat(groups).hasSize(1);
        assertThat(groups).extracting("name").contains("testgroup");
    }

    @Test
    public void should_searchGroups_when_paginated() {
        List<KeycloakGroup> groupsPage1 = keycloakClient.searchGroups("group", 0, 1);

        assertThat(groupsPage1).hasSize(1);
        assertThat(groupsPage1).extracting("name").contains("salesgroup");

        List<KeycloakGroup> groupsPage2 = keycloakClient.searchGroups("group", 1, 1);

        assertThat(groupsPage2).hasSize(1);
        assertThat(groupsPage2).extracting("name").contains("testgroup");
    }

    @Test
    public void should_getUserRoles() {
        List<KeycloakUser> users = keycloakClient.searchUsers("hruser", 0, 50);
        List<KeycloakRoleMapping> roles = keycloakClient.getUserRoleMapping(users.get(0).getId());

        assertThat(roles).hasSize(3);
        assertThat(roles).extracting("name").contains("ACTIVITI_USER");
        assertThat(roles).extracting("name").contains("uma_authorization");
        assertThat(roles).extracting("name").contains("offline_access");
    }

    @Test
    public void should_getGroupRoles() {
        List<KeycloakGroup> users = keycloakClient.searchGroups("salesgroup", 0, 50);
        List<KeycloakRoleMapping> roles = keycloakClient.getGroupRoleMapping(users.get(0).getId());

        assertThat(roles).hasSize(1);
        assertThat(roles).extracting("name").contains("ACTIVITI_USER");
    }

    @Test
    public void should_getClients() {
        List<KeycloakClientRepresentation> clients = keycloakClient.searchClients(null,0, 50);

        assertThat(clients).extracting("clientId").contains("activiti");
        assertThat(clients).extracting("clientId").contains("activiti-keycloak");
    }

    @Test
    public void should_getClients_when_paginated() {
        List<KeycloakClientRepresentation> clientPage1 = keycloakClient.searchClients(null,0, 1);
        assertThat(clientPage1).hasSize(1);
        assertThat(clientPage1).extracting("clientId").contains("account");

        List<KeycloakClientRepresentation> clientPage2 = keycloakClient.searchClients(null, 1, 1);

        assertThat(clientPage2).hasSize(1);
        assertThat(clientPage2).extracting("clientId").contains("account-console");
    }

    @Test
    public void should_getClientsFilteredByClientId() {
        List<KeycloakClientRepresentation> clients = keycloakClient.searchClients("activiti-keycloak", 0, 50);

        assertThat(clients).hasSize(1);
        assertThat(clients).extracting("clientId").contains("activiti-keycloak");
    }

    @Test
    public void should_getUserClientRoles() {
        List<KeycloakUser> users = keycloakClient.searchUsers("testActivitiAdmin", 0, 50);
        List<KeycloakClientRepresentation> clients = keycloakClient.searchClients("activiti", 0, 50);

        List<KeycloakRoleMapping> roles = keycloakClient.getUserClientRoleMapping(users.get(0).getId(), clients.get(0).getId());

        assertThat(roles).hasSize(3);
        assertThat(roles).extracting("name").contains("ACTIVITI_ADMIN");
        assertThat(roles).extracting("name").contains("uma_authorization");
        assertThat(roles).extracting("name").contains("offline_access");
    }

    @Test
    public void should_getGroupClientRoles() {
        List<KeycloakGroup> groups = keycloakClient.searchGroups("salesgroup", 0, 50);
        List<KeycloakClientRepresentation> clients = keycloakClient.searchClients("activiti", 0, 50);

        List<KeycloakRoleMapping> roles = keycloakClient
            .getGroupClientRoleMapping(groups.get(0).getId(), clients.get(0).getId());

        assertThat(roles).hasSize(1);
        assertThat(roles).extracting("name").contains("ACTIVITI_USER");
    }

}
