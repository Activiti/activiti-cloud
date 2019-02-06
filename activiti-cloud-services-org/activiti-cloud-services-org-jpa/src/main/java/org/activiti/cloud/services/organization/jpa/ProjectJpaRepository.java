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

package org.activiti.cloud.services.organization.jpa;

import java.util.Optional;
import java.util.UUID;

import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for {@link ProjectEntity} entity
 */
@RepositoryRestResource(path = "projects",
        collectionResourceRel = "projects",
        itemResourceRel = "projects",
        exported = false)
public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, String>,
                                              ProjectRepository<ProjectEntity> {

    @Override
    default Page<ProjectEntity> getProjects(Pageable pageable) {
        return findAll(pageable);
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
    default void deleteProject(ProjectEntity project) {
        delete(project);
    }
}
