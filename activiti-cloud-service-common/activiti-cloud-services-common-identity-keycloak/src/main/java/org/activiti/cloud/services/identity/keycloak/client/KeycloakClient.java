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
import java.util.List;
import org.activiti.cloud.security.feign.configuration.ClientCredentialsAuthConfiguration;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakClientRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakGroup;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakUser;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "keycloak",
    url = "${keycloak.auth-server-url}/admin/realms/${keycloak.realm}/",
    configuration = ClientCredentialsAuthConfiguration.class)
public interface KeycloakClient {

    @RequestMapping(method = RequestMethod.GET, value = "/users")
    @Headers("Content-Type: application/json")
    List<KeycloakUser> searchUsers(@RequestParam(value = "search") String search,
                                   @RequestParam(value = "first") Integer first, @RequestParam(value = "max") Integer max);

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}/role-mappings/realm/composite")
    @Headers("Content-Type: application/json")
    @Cacheable("userRoleMapping")
    List<KeycloakRoleMapping> getUserRoleMapping(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}/groups")
    @Headers("Content-Type: application/json")
    @Cacheable("userGroups")
    List<KeycloakGroup> getUserGroups(@PathVariable("id") String userId);

    @RequestMapping(method = RequestMethod.GET, value = "/groups")
    @Headers("Content-Type: application/json")
    List<KeycloakGroup> searchGroups(@RequestParam(value = "search") String search,
                                     @RequestParam(value = "first") Integer first, @RequestParam(value = "max") Integer max);

    @RequestMapping(method = RequestMethod.GET, value = "/groups/{id}/role-mappings/realm/composite")
    @Headers("Content-Type: application/json")
    @Cacheable("groupRoleMapping")
    List<KeycloakRoleMapping> getGroupRoleMapping(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.GET, value = "/clients")
    @Headers("Content-Type: application/json")
    List<KeycloakClientRepresentation> searchClients(
        @RequestParam(value = "clientId", required = false) String clientId,
        @RequestParam(value = "first") Integer first, @RequestParam(value = "max") Integer max);

    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}/role-mappings/clients/{client}/composite")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> getUserClientRoleMapping(@PathVariable("id") String id,
                                                       @PathVariable("client") String client);

    @RequestMapping(method = RequestMethod.GET, value = "/groups/{id}/role-mappings/clients/{client}/composite")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> getGroupClientRoleMapping(@PathVariable("id") String id,
                                                        @PathVariable("client") String client);

    @RequestMapping(method = RequestMethod.GET, value = "clients/{id}/roles")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> getClientRoles(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.POST, value = "/users/{id}/role-mappings/clients/{client}")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> addUserClientRoleMapping(@PathVariable("id") String id,
                                                       @PathVariable("client") String client, @RequestBody List<KeycloakRoleMapping> roles);

    @RequestMapping(method = RequestMethod.POST, value = "/groups/{id}/role-mappings/clients/{client}")
    @Headers("Content-Type: application/json")
    List<KeycloakRoleMapping> addGroupClientRoleMapping(@PathVariable("id") String id,
                                                        @PathVariable("client") String client, @RequestBody List<KeycloakRoleMapping> roles);

    @RequestMapping(method = RequestMethod.GET, value = "clients/{id}/roles/{role-name}/users")
    @Headers("Content-Type: application/json")
    List<KeycloakUser> getUsersClientRoleMapping(@PathVariable("id") String id,
                                                 @PathVariable("role-name") String roleName);

    @RequestMapping(method = RequestMethod.GET, value = "clients/{id}/roles/{role-name}/groups")
    @Headers("Content-Type: application/json")
    List<KeycloakGroup> getGroupsClientRoleMapping(@PathVariable("id") String id,
                                                   @PathVariable("role-name") String roleName);

    @RequestMapping(method = RequestMethod.GET, value = "/groups/{groupId}/members")
    @Headers("Content-Type: application/json")
    List<KeycloakUser> getUsersByGroupId(@PathVariable("groupId") String groupId);
}
