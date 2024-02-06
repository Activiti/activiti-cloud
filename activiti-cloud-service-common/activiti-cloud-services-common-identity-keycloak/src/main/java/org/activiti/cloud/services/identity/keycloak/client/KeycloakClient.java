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
package org.activiti.cloud.services.identity.keycloak.client;

import feign.Headers;
import feign.Response;

import java.util.List;

import org.activiti.cloud.services.identity.keycloak.model.KeycloakClientRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakCredentialRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakCredentialRequestRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakMappingsRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakUser;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

public interface KeycloakClient {
    @RequestMapping(method = RequestMethod.GET, value = "/users")
    @Headers("Content-Type: application/json")
    List<KeycloakUser> searchUsers(
        @RequestParam(value = "search") String search,
        @RequestParam(value = "first") Integer first,
        @RequestParam(value = "max") Integer max
    );

    @RequestMapping(method = RequestMethod.GET, value = "/users")
    @Headers("Content-Type: application/json")
    List<KeycloakUser> searchUsersByUsername(@RequestParam(value = "username") String username);

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}/role-mappings/realm/composite")
    @Headers("Content-Type: application/json")
    @Cacheable("userRoleMapping")
    List<KeycloakRoleMapping> getUserRoleMapping(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}/role-mappings/realm/available")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> getUserRoleMappingAvailable(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.POST, value = "/users/{id}/role-mappings/realm")
    @Headers("Content-Type: application/json")
    void addRealmLevelUserRoleMapping(@PathVariable("id") String id, List<KeycloakRoleMapping> roles);

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}/groups")
    @Headers("Content-Type: application/json")
    @Cacheable("userGroups")
    List<KeycloakGroup> getUserGroups(@PathVariable("id") String userId);

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}/role-mappings")
    @Headers("Content-Type: application/json")
    KeycloakMappingsRepresentation getUserRoles(@PathVariable("id") String userId);

    @RequestMapping(method = RequestMethod.GET, value = "/groups")
    @Headers("Content-Type: application/json")
    List<KeycloakGroup> searchGroups(
        @RequestParam(value = "search") String search,
        @RequestParam(value = "first") Integer first,
        @RequestParam(value = "max") Integer max
    );

    @RequestMapping(method = RequestMethod.GET, value = "/groups/{id}/role-mappings/realm/composite")
    @Headers("Content-Type: application/json")
    @Cacheable("groupRoleMapping")
    List<KeycloakRoleMapping> getGroupRoleMapping(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "/groups/{id}/role-mappings")
    @Headers("Content-Type: application/json")
    KeycloakMappingsRepresentation getAllGroupRoleMapping(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "/clients")
    @Headers("Content-Type: application/json")
    List<KeycloakClientRepresentation> searchClients(
        @RequestParam(value = "clientId", required = false) String clientId,
        @RequestParam(value = "first") Integer first,
        @RequestParam(value = "max") Integer max
    );

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}/role-mappings/clients/{client}/composite")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> getUserClientRoleMapping(
        @PathVariable("id") String id,
        @PathVariable("client") String client
    );

    @RequestMapping(method = RequestMethod.GET, value = "/groups/{id}/role-mappings/clients/{client}/composite")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> getGroupClientRoleMapping(
        @PathVariable("id") String id,
        @PathVariable("client") String client
    );

    @RequestMapping(method = RequestMethod.GET, value = "clients/{id}/roles")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> getClientRoles(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.POST, value = "/users/{id}/role-mappings/clients/{client}")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> addUserClientRoleMapping(
        @PathVariable("id") String id,
        @PathVariable("client") String client,
        @RequestBody List<KeycloakRoleMapping> roles
    );

    @RequestMapping(method = RequestMethod.POST, value = "/groups/{id}/role-mappings/clients/{client}")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> addGroupClientRoleMapping(
        @PathVariable("id") String id,
        @PathVariable("client") String client,
        @RequestBody List<KeycloakRoleMapping> roles
    );

    @RequestMapping(method = RequestMethod.DELETE, value = "/users/{id}/role-mappings/clients/{client}")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> removeUserClientRoleMapping(
        @PathVariable("id") String id,
        @PathVariable("client") String client,
        @RequestBody List<KeycloakRoleMapping> roles
    );

    @RequestMapping(method = RequestMethod.DELETE, value = "/groups/{id}/role-mappings/clients/{client}")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> removeGroupClientRoleMapping(
        @PathVariable("id") String id,
        @PathVariable("client") String client,
        @RequestBody List<KeycloakRoleMapping> roles
    );

    default List<KeycloakUser> getUsersClientRoleMapping(String id, String roleName) {
        return getUsersClientRoleMapping(id, roleName, 0, 100);
    }

    @RequestMapping(method = RequestMethod.GET, value = "clients/{id}/roles/{role-name}/users")
    @Headers("Content-Type: application/json")
    List<KeycloakUser> getUsersClientRoleMapping(
        @PathVariable("id") String id,
        @PathVariable("role-name") String roleName,
        @RequestParam(value = "first") Integer first,
        @RequestParam(value = "max") Integer max
    );

    @RequestMapping(method = RequestMethod.GET, value = "clients/{id}/service-account-user")
    @Headers("Content-Type: application/json")
    KeycloakUser getServiceAccountUserOfClient(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "clients/{id}/roles/{role-name}/groups")
    @Headers("Content-Type: application/json")
    List<KeycloakGroup> getGroupsClientRoleMapping(
        @PathVariable("id") String id,
        @PathVariable("role-name") String roleName
    );

    @RequestMapping(method = RequestMethod.GET, value = "clients/{id}/roles/{role-name}")
    @Headers("Content-Type: application/json")
    KeycloakRoleMapping getRoleRepresentationForClient(
        @PathVariable("id") String id,
        @PathVariable("role-name") String roleName
    );

    @RequestMapping(method = RequestMethod.POST, value = "clients/{id}/roles")
    @Headers("Content-Type: application/json")
    KeycloakRoleMapping createRoleRepresentationForClient(
        @PathVariable("id") String id,
        @RequestBody KeycloakRoleMapping keycloakRoleMapping
    );

    default List<KeycloakUser> getUsersByGroupId(String groupId) {
        return getUsersByGroupId(groupId, 0, 100);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/groups/{groupId}/members")
    @Headers("Content-Type: application/json")
    List<KeycloakUser> getUsersByGroupId(
        @PathVariable("groupId") String groupId,
        @RequestParam(value = "first") Integer first,
        @RequestParam(value = "max") Integer max
    );

    @RequestMapping(method = RequestMethod.GET, value = "/groups/{id}")
    @Headers("Content-Type: application/json")
    KeycloakGroup getGroupById(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "/groups")
    @Headers("Content-Type: application/json")
    List<KeycloakGroup> getAllGroups();

    @RequestMapping(method = RequestMethod.GET, value = "/users")
    @Headers("Content-Type: application/json")
    List<KeycloakUser> getAllUsers(@RequestParam(name = "max", required = false, defaultValue = "100") Integer max);

    @RequestMapping(method = RequestMethod.GET, value = "/users/count")
    @Headers("Content-Type: application/json")
    Integer countAllUsers();

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}")
    @Headers("Content-Type: application/json")
    KeycloakUser getUserById(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "/group-by-path/{path}")
    @Headers("Content-Type: application/json")
    KeycloakGroup getGroupByPath(@PathVariable("path") String path);

    @RequestMapping(method = RequestMethod.GET, value = "/clients")
    @Headers("Content-Type: application/json")
    List<KeycloakClientRepresentation> findByClientId(
        @RequestParam(value = "clientId", required = false) String clientId
    );

    @RequestMapping(method = RequestMethod.GET, value = "/clients/{id}")
    @Headers("Content-Type: application/json")
    KeycloakClientRepresentation getClientById(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.DELETE, value = "/clients/{id}")
    @Headers("Content-Type: application/json")
    void deleteClient(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.POST, value = "/clients")
    @Headers("Content-Type: application/json")
    Response createClient(@RequestBody KeycloakClientRepresentation keycloakClientRepresentation);

    @RequestMapping(method = RequestMethod.PUT, value = "/clients/{id}")
    @Headers("Content-Type: application/json")
    void updateClient(
        @PathVariable("id") String id,
        @RequestBody KeycloakClientRepresentation keycloakClientRepresentation
    );

    @RequestMapping(method = RequestMethod.POST, value = "/clients/{id}/client-secret")
    @Headers("Content-Type: application/json")
    KeycloakCredentialRepresentation createClientSecretById(@RequestBody KeycloakCredentialRequestRepresentation requestRepresentation, @PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "/clients/{id}/client-secret")
    @Headers("Content-Type: application/json")
    KeycloakCredentialRepresentation getClientSecretById(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}/role-mappings/clients/{client}/available")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> getClientLevelRoleMappingAvailable(@PathVariable("id") String id, @PathVariable("client") String clientId);
}
