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

package org.activiti.cloud.qa.steps;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.Group;
import org.activiti.cloud.qa.rest.feign.EnableFeignContext;
import org.activiti.cloud.qa.service.ModelingGroupsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.hateoas.Link.REL_SELF;

/**
 * Modeling steps
 */
@EnableFeignContext
public class ModelingSteps {

    private final String MODELING_CURRENT_GROUP_URI = "currentGroupUri";

    private final String GROUP_SUBGROUPS_REL = "subgroups";

    @Autowired
    private ModelingGroupsService modelingGroupsService;

    @Step
    public void ensureGroupExists(String groupName) {
        if (!groupExists(groupName)) {
            createGroup(groupName);
        }
    }

    @Step
    public void createGroup(String groupName) {
        String groupId = UUID.randomUUID().toString();
        modelingGroupsService
                .create(new Group(groupId,
                                  groupName));

        Resource<Group> createdGroup = modelingGroupsService.findById(groupId);
        String currentGroupUri =
                Serenity.sessionVariableCalled(MODELING_CURRENT_GROUP_URI);

        if (currentGroupUri != null) {
            Resource<Group> currentGroup = modelingGroupsService.findByUri(currentGroupUri);
            modelingGroupsService.addRelationByUri(
                    currentGroup.getLink(GROUP_SUBGROUPS_REL).getHref(),
                    createdGroup.getLink(REL_SELF).getHref());
        }
    }

    @Step
    public void openGroup(String groupName,
                          Collection<Resource<Group>> groups) {
        assertThat(groups).isNotEmpty();
        Optional<Resource<Group>> currentGroup = groups
                .stream()
                .filter(group -> group.getContent().getName().equals(groupName))
                .findFirst();
        assertThat(currentGroup.isPresent()).isTrue();

        Serenity.setSessionVariable(MODELING_CURRENT_GROUP_URI)
                .to(currentGroup.get().getLink(REL_SELF).getHref());
    }

    @Step
    public Collection<Resource<Group>> getAllGroups() {
        return modelingGroupsService
                .findAll()
                .getContent();
    }

    @Step
    public void checkGroupExists(String groupName) {
        assertThat(groupExists(groupName)).isTrue();
    }

    @Step
    public void checkSubGroupExistsInCurrentGroup(String subGroupName,
                                                  String currentGroupName) {
        String currentGroupUri =
                Serenity.sessionVariableCalled(MODELING_CURRENT_GROUP_URI);

        assertThat(currentGroupUri).isNotNull();

        Resource<Group> currentGroup = modelingGroupsService.findByUri(currentGroupUri);
        assertThat(currentGroup.getContent().getName()).isEqualTo(currentGroupName);

        PagedResources<Resource<Group>> subGroups =
                modelingGroupsService.findAllByUri(currentGroup.getLink(GROUP_SUBGROUPS_REL).getHref());
        assertThat(subGroups
                           .getContent()
                           .stream()
                           .map(Resource::getContent)
                           .map(Group::getName)
                           .filter(name -> subGroupName.equals(name))
                           .findFirst()
                           .isPresent()
        ).isTrue();
    }

    @Step
    public void deleteGroup(String groupName) {
        deleteGroups(Collections.singletonList(groupName));
    }

    @Step
    public void deleteGroups(List<String> groupNames) {
        getAllGroups()
                .stream()
                .filter(resourceGroup -> groupNames.contains(resourceGroup.getContent().getName()))
                .map(resourceGroup -> resourceGroup.getLink(REL_SELF))
                .map(Link::getHref)
                .collect(Collectors.toList())
                .forEach(modelingGroupsService::deleteByUri);
    }

    private boolean groupExists(String groupName) {
        return getAllGroups()
                .stream()
                .map(Resource::getContent)
                .map(Group::getName)
                .filter(name -> groupName.equals(name))
                .findFirst()
                .isPresent();
    }
}
