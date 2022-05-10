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
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.identity.GroupSearchParams;
import org.activiti.cloud.identity.IdentityManagementService;
import org.activiti.cloud.identity.UserSearchParams;
import org.activiti.cloud.identity.web.assembler.ModelRepresentationGroupAssembler;
import org.activiti.cloud.identity.web.assembler.ModelRepresentationUserAssembler;
import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/identity", produces = MediaType.APPLICATION_JSON_VALUE)
public class IdentityManagementController {

    private final IdentityManagementService identityManagementService;
    private final AlfrescoPagedModelAssembler<User> pagedCollectionUserAssembler;
    private final AlfrescoPagedModelAssembler<Group> pagedCollectionGroupAssembler;
    private final ModelRepresentationGroupAssembler modelRepresentationGroupAssembler;
    private final ModelRepresentationUserAssembler modelRepresentationUserAssembler;

    public IdentityManagementController(IdentityManagementService identityManagementService,
                                        AlfrescoPagedModelAssembler<User> pagedCollectionUserAssembler,
                                        AlfrescoPagedModelAssembler<Group> pagedCollectionGroupAssembler,
                                        ModelRepresentationGroupAssembler modelRepresentationGroupAssembler,
                                        ModelRepresentationUserAssembler modelRepresentationUserAssembler) {
        this.identityManagementService = identityManagementService;
        this.pagedCollectionUserAssembler = pagedCollectionUserAssembler;
        this.pagedCollectionGroupAssembler = pagedCollectionGroupAssembler;
        this.modelRepresentationGroupAssembler = modelRepresentationGroupAssembler;
        this.modelRepresentationUserAssembler = modelRepresentationUserAssembler;
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public PagedModel<EntityModel<User>> getUsers(@RequestParam(value = "search", required = false) String search,
                                                  @RequestParam(value = "role", required = false)  Set<String> roles,
                                                  Pageable pageable) {

        UserSearchParams userSearchParams = new UserSearchParams();
        userSearchParams.setSearch(search);
        userSearchParams.setRoles(roles);
        userSearchParams.setFromPageable(pageable);

        List<User> users = identityManagementService.findUsers(userSearchParams);
        Page<User> page = new PageImpl<>(users, pageable, users.size());
        return pagedCollectionUserAssembler.toModel(pageable,
                                                     page,
                                                    modelRepresentationUserAssembler);
    }

    @RequestMapping(value = "/groups", method = RequestMethod.GET)
    public PagedModel<EntityModel<Group>> getGroups(@RequestParam(value = "search", required = false) String search,
                                                  @RequestParam(value = "role", required = false)  Set<String> roles,
                                                  Pageable pageable) {

        GroupSearchParams groupSearchParams = new GroupSearchParams();
        groupSearchParams.setSearch(search);
        groupSearchParams.setRoles(roles);
        groupSearchParams.setFromPageable(pageable);

        List<Group> groups = identityManagementService.findGroups(groupSearchParams);
        Page<Group> page = new PageImpl<>(groups, pageable, groups.size());
        return pagedCollectionGroupAssembler.toModel(pageable,
                                                    page,
                                                    modelRepresentationGroupAssembler);
    }

}
