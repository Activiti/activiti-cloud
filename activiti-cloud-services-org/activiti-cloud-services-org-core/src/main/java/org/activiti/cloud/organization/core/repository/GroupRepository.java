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

import java.util.List;
import java.util.Optional;

import org.activiti.cloud.organization.core.model.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for {@link Group} entities repository
 */
public interface GroupRepository {

    Page<Group> getTopLevelGroups(Pageable page);

    Page<Group> getGroups(String parentId,
                          Pageable pageable);

    Optional<Group> findGroupById(String groupId);

    Group createGroup(Group group);

    Group updateGroup(Group groupToUpdate);

    void deleteGroup(Group group);

    default Group createGroup(Group parentGroup, Group group) {
        group.setParent(parentGroup);
        return createGroup(group);
    }

    default Group updateGroup(Group groupToUpdate,
                              Group newGroup) {
        groupToUpdate.setName(newGroup.getName());
        return updateGroup(groupToUpdate);
    }

    default Optional<Group> findGroupByLink(String link) {
        return findGroupById(link.substring(link.lastIndexOf('/') + 1));
    }

    default void createSubgroupReference(Group group,
                                         List<String> subgroupLinks) {
        subgroupLinks
                .stream()
                .distinct()
                .map(this::findGroupByLink)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(subgroup -> {
                    subgroup.setParent(group);
                    updateGroup(subgroup);
                });
    }

}
