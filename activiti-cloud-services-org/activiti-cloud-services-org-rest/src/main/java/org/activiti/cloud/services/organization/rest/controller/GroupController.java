/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.organization.rest.controller;

import java.util.stream.Collectors;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.organization.core.model.Group;
import org.activiti.cloud.organization.core.repository.GroupRepository;
import org.activiti.cloud.services.organization.rest.assembler.GroupResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller for {@link Group} resources
 */
@RestController
@RequestMapping(
        value = "/v1/groups",
        produces = {
                HAL_JSON_VALUE,
                APPLICATION_JSON_VALUE
        }
)
public class GroupController {

    private final GroupRepository groupRepository;

    private final GroupResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Group> pagedResourcesAssembler;

    @Autowired
    public GroupController(GroupRepository groupRepository,
                           GroupResourceAssembler resourceAssembler,
                           AlfrescoPagedResourcesAssembler<Group> pagedResourcesAssembler) {
        this.groupRepository = groupRepository;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @RequestMapping(method = GET)
    public PagedResources<Resource<Group>> getTopLevelGroups(Pageable pageable) {
        return pagedResourcesAssembler.toResource(pageable,
                                                  groupRepository.getTopLevelGroups(pageable),
                                                  resourceAssembler);
    }

    @RequestMapping(method = GET, path = "/{groupId}/subgroups")
    public PagedResources<Resource<Group>> getSubgroups(@PathVariable String groupId,
                                                        Pageable pageable) {
        return pagedResourcesAssembler.toResource(pageable,
                                                  groupRepository.getGroups(groupId,
                                                                            pageable),
                                                  resourceAssembler);
    }

    @RequestMapping(method = {PUT, PATCH}, path = "/{groupId}/subgroups", consumes = TEXT_URI_LIST_VALUE)
    @ResponseStatus(NO_CONTENT)
    public void createSubgroupReference(@PathVariable String groupId,
                                        @RequestBody Resources<Object> subgroupLinks) {
        groupRepository.createSubgroupReference(
                findGroupById(groupId),
                subgroupLinks
                        .getLinks()
                        .stream()
                        .map(Link::getHref)
                        .collect(Collectors.toList()));
    }

    @RequestMapping(method = GET, path = "/{groupId}")
    public Resource<Group> getGroup(@PathVariable String groupId) {
        return resourceAssembler.toResource(findGroupById(groupId));
    }

    @RequestMapping(method = POST)
    @ResponseStatus(CREATED)
    public Resource<Group> createGroup(@RequestBody Group group) {
        return resourceAssembler.toResource(groupRepository.createGroup(group));
    }

    @RequestMapping(method = POST, path = "/{groupId}/subgroups")
    @ResponseStatus(CREATED)
    public Resource<Group> createSubgroup(@PathVariable String groupId,
                                          @RequestBody Group group) {
        return resourceAssembler.toResource(groupRepository.createGroup(findGroupById(groupId),
                                                                        group));
    }

    @RequestMapping(method = PUT, path = "/{groupId}")
    public Resource<Group> updateGroup(@PathVariable String groupId,
                                       @RequestBody Group group) {
        Group groupToUpdate = findGroupById(groupId);
        return resourceAssembler.toResource(groupRepository.updateGroup(groupToUpdate,
                                                                        group));
    }

    @RequestMapping(method = DELETE, path = "/{groupId}")
    public void deleteGroup(@PathVariable String groupId) {
        groupRepository.deleteGroup(findGroupById(groupId));
    }

    public Group findGroupById(String groupId) {
        return groupRepository
                .findGroupById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization group not found: " + groupId));
    }
}
