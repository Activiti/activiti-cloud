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
package org.activiti.cloud.services.modeling.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.modeling.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * JPA Repository for {@link ProjectEntity} entity
 */
@RepositoryRestResource(
        path = "projects",
        collectionResourceRel = "projects",
        itemResourceRel = "projects",
        exported = false
)
public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, String>, ProjectRepository<ProjectEntity> {
    Page<ProjectEntity> findAllByDisplayNameContainingIgnoreCase(String name, Pageable pageable);

    Page<ProjectEntity> findAllByIdIn(Collection<String> filteredProjectIds, Pageable pageable);

    Page<ProjectEntity> findAllByDisplayNameContainingIgnoreCaseAndIdIn(
            String name,
            Collection<String> filteredProjectIds,
            Pageable pageable
    );

    @Override
    default Page<ProjectEntity> getProjects(Pageable pageable, String nameToFilter, List<String> filteredProjectIds) {
        if (nameToFilter != null && filteredProjectIds != null) {
            return findAllByDisplayNameContainingIgnoreCaseAndIdIn(nameToFilter, filteredProjectIds, pageable);
        } else if (nameToFilter != null) {
            return findAllByDisplayNameContainingIgnoreCase(nameToFilter, pageable);
        } else if (filteredProjectIds != null) {
            return findAllByIdIn(filteredProjectIds, pageable);
        } else {
            return findAll(pageable);
        }
    }

    @Override
    default Optional<ProjectEntity> findProjectById(String projectId) {
        return findById(projectId);
    }

    @Override
    default ProjectEntity createProject(ProjectEntity project) {
        return save(project);
    }

    @Override
    default ProjectEntity updateProject(ProjectEntity projectToUpdate) {
        return save(projectToUpdate);
    }

    @Override
    default ProjectEntity copyProject(ProjectEntity projectToCopy, String newProjectName, String newProjectKey) {
        ProjectEntity projectEntityClone = new ProjectEntity(newProjectName, newProjectKey);
        projectEntityClone.setDescription(projectToCopy.getDescription());
        return save(projectEntityClone);
    }

    @Override
    default void deleteProject(ProjectEntity project) {
        delete(project);
    }
}
