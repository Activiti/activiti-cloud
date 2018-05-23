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

import org.activiti.cloud.organization.core.model.Model;
import org.activiti.cloud.organization.core.model.Project;
import org.activiti.cloud.organization.core.repository.ModelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * JPA Repository for {@link Model} entity
 */
@RepositoryRestResource(path = "models",
        collectionResourceRel = "models",
        itemResourceRel = "models")
public interface ModelJpaRepository extends JpaRepository<Model, String>,
                                            ModelRepository {

    Page<Model> findAllByProjectIdIsNull(Pageable pageable);

    Page<Model> findAllByProjectId(String projectId,
                                   Pageable pageable);

    @Override
    default Page<Model> getTopLevelModels(Pageable pageable) {
        return findAllByProjectIdIsNull(pageable);
    }

    @Override
    default Page<Model> getModels(Project project,
                                    Pageable pageable) {
        return findAllByProjectId(project.getId(),
                                  pageable);
    }

    @Override
    default Optional<Model> findModelById(String id) {
        return findById(id);
    }

    @Override
    default Model createModel(Model model) {
        return save(model);
    }

    @Override
    default Model updateModel(Model model) {
        return save(model);
    }

    @Override
    default void deleteModel(Model model) {
        delete(model);
    }
}
