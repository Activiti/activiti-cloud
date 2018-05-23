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

package org.activiti.cloud.organization.core.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.activiti.cloud.organization.core.model.Group;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link GroupRepository}
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupRepositoryTest {

    @Spy
    private GroupRepository groupRepository;

    @Captor
    private ArgumentCaptor<Group> groupArgumentCaptor;

    @Test
    public void testCreateGroup() {
        // GIVEN
        Group parentGroup = new Group("parent_group_id",
                                      "Parent Group");
        Group childGroup = new Group("child_group_id",
                                     "Child Group");

        // WHEN
        groupRepository.createGroup(parentGroup,
                                    childGroup);

        // THEN
        verify(groupRepository,
               times(1))
                .createGroup(groupArgumentCaptor.capture());

        Group createdGroup = groupArgumentCaptor.getValue();
        assertThat(createdGroup).isNotNull();
        assertThat(createdGroup.getId()).isEqualTo("child_group_id");
        assertThat(createdGroup.getName()).isEqualTo("Child Group");
        assertThat(createdGroup.getParent()).isNotNull();
        assertThat(createdGroup.getParent().getId()).isEqualTo("parent_group_id");
        assertThat(createdGroup.getParent().getName()).isEqualTo("Parent Group");
    }

    @Test
    public void testUpdateGroup() {
        // GIVEN
        Group groupToUpdate = new Group("group_id",
                                        "Group Name");
        Group group = new Group("new_group_id",
                                "New Group Name");

        // WHEN
        groupRepository.updateGroup(groupToUpdate,
                                    group);

        // THEN
        verify(groupRepository,
               times(1))
                .updateGroup(groupArgumentCaptor.capture());

        Group updatedGroup = groupArgumentCaptor.getValue();
        assertThat(updatedGroup).isNotNull();
        assertThat(updatedGroup.getId()).isEqualTo("group_id");
        assertThat(updatedGroup.getName()).isEqualTo("New Group Name");
    }

    @Test
    public void testFindGroupByLink() {
        // GIVEN
        String groupSelfLink = "http://localhost:8080:/groups/group_id";

        // WHEN
        groupRepository.findGroupByLink(groupSelfLink);

        // THEN
        verify(groupRepository,
               times(1))
                .findGroupById(eq("group_id"));
    }

    @Test
    public void testCreateSubgroupReference() {
        // GIVEN
        Group group = new Group("parent_group_id",
                                "Parent Group Name");

        Group subgroup1 = new Group("subgroup1",
                                    "Subgroup 1");

        Group subgroup2 = new Group("subgroup2",
                                    "Subgroup 2");

        List<String> subgroupLinks = Arrays.asList(
                "http://localhost:8080/groups/subgroup1",
                "http://localhost:8080/groups/wrong_group_id",
                "http://localhost:8080/groups/subgroup2"
        );

        doReturn(Optional.of(subgroup1)).when(groupRepository).findGroupById(eq("subgroup1"));
        doReturn(Optional.of(subgroup2)).when(groupRepository).findGroupById(eq("subgroup2"));

        // WHEN
        groupRepository.createSubgroupReference(group,
                                                subgroupLinks);

        // THEN
        verify(groupRepository,
               times(3))
                .findGroupById(anyString());

        verify(groupRepository,
               times(2))
                .updateGroup(groupArgumentCaptor.capture());

        List<Group> updatedGroups = groupArgumentCaptor.getAllValues();
        assertThat(updatedGroups).hasSize(2);

        assertThat(updatedGroups
                           .stream()
                           .map(Group::getId)
                           .collect(Collectors.toList()))
                .containsExactly("subgroup1",
                                 "subgroup2");

        assertThat(updatedGroups
                           .stream()
                           .map(Group::getParent)
                           .filter(Objects::nonNull)
                           .map(Group::getId)
                           .collect(Collectors.toList()))
                .containsExactly("parent_group_id",
                                 "parent_group_id");
    }
}
