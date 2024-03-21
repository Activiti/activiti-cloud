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
package org.activiti.cloud.identity.web.controller;

import java.util.List;
import java.util.Set;
import org.activiti.cloud.identity.GroupSearchParams;
import org.activiti.cloud.identity.IdentityManagementService;
import org.activiti.cloud.identity.UserSearchParams;
import org.activiti.cloud.identity.UserTypeSearchParam;
import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.SecurityRequestBodyRepresentation;
import org.activiti.cloud.identity.model.SecurityResponseRepresentation;
import org.activiti.cloud.identity.model.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "${activiti.cloud.services.identity.url:/v1}", produces = MediaType.APPLICATION_JSON_VALUE)
public class IdentityManagementController {

    private final IdentityManagementService identityManagementService;

    public IdentityManagementController(IdentityManagementService identityManagementService) {
        this.identityManagementService = identityManagementService;
    }

    @GetMapping(value = "/users")
    @Cacheable("userSearch")
    public List<User> getUsers(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "role", required = false) Set<String> roles,
        @RequestParam(value = "group", required = false) Set<String> groups,
        @RequestParam(value = "type", required = false) String type,
        @RequestParam(value = "application", required = false) String application,
        @RequestParam(value = "hideDeactivatedUser", required = false) boolean filterDeactivatedUsers
    ) {
        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch(search);
        userSearchParams.setGroups(groups);
        userSearchParams.setType(type == null ? null : UserTypeSearchParam.convertFromStringOrThrow(type));
        userSearchParams.setRoles(roles);
        userSearchParams.setApplication(application);
        userSearchParams.setFilterDeactivatedUsers(filterDeactivatedUsers);

        return identityManagementService.findUsers(userSearchParams);
    }

    @GetMapping(value = "/groups")
    @Cacheable("groupSearch")
    public List<Group> getGroups(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "role", required = false) Set<String> roles,
        @RequestParam(value = "application", required = false) String application
    ) {
        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch(search);
        groupSearchParams.setRoles(roles);
        groupSearchParams.setApplication(application);

        return identityManagementService.findGroups(groupSearchParams);
    }

    @PostMapping(value = "/permissions/{application}")
    public void addApplicationPermissions(
        @PathVariable String application,
        @RequestBody List<SecurityRequestBodyRepresentation> securityRequestBodyRepresentations
    ) {
        identityManagementService.addApplicationPermissions(application, securityRequestBodyRepresentations);
    }

    @GetMapping(value = "/permissions/{application}")
    public List<SecurityResponseRepresentation> getApplicationPermissions(
        @PathVariable String application,
        @RequestParam(value = "role", required = false) Set<String> roles
    ) {
        return identityManagementService.getApplicationPermissions(application, roles);
    }

    @GetMapping(value = "/users/{id}")
    public User getUsersById(@PathVariable String id) {
        return identityManagementService.findUserById(id);
    }
}
