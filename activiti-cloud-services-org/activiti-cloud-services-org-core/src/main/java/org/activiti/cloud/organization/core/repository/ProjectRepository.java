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
import org.activiti.cloud.organization.core.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for {@link Project} entities repository
 */
public interface ProjectRepository {

    Page<Project> getTopLevelProjects(Pageable pageable);

    Page<Project> getProjects(Group group,
                              Pageable pageable);

    Optional<Project> findProjectById(String projectId);

    Project createProject(Project project);

    Project updateProject(Project projectToUpdate);

    void deleteProject(Project project);

    default Project createProject(Group group,
                                  Project project) {
        project.setGroup(group);
        return createProject(project);
    }

    default Project updateProject(Project projectToUpdate,
                                  Project newProject) {
        projectToUpdate.setName(newProject.getName());
        return updateProject(projectToUpdate);
    }

    default Optional<Project> findProjectByLink(String link) {
        return findProjectById(link.substring(link.lastIndexOf('/') + 1));
    }

    default void createProjectsReference(Group group,
                                         List<String> projectsLinks) {
        projectsLinks
                .stream()
                .distinct()
                .map(this::findProjectByLink)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(project -> {
                    project.setGroup(group);
                    updateProject(project);
                });
    }

}
