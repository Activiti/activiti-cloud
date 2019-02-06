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

import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.activiti.cloud.services.organization.entity.ModelVersionEntity;
import org.activiti.cloud.services.organization.entity.ProjectEntity;
import org.activiti.cloud.services.organization.jpa.version.VersionedJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for {@link ModelEntity} entity
 */
@RepositoryRestResource(path = "models",
        collectionResourceRel = "models",
        itemResourceRel = "models",
        exported = false)
public interface ModelJpaRepository extends VersionedJpaRepository<ModelEntity, String, ModelVersionEntity>,
                                            ModelRepository<ProjectEntity, ModelEntity> {

    Page<ModelEntity> findAllByProjectIdAndTypeEquals(String projectId,
                                                      String modelTypeFilter,
                                                      Pageable pageable);

    @Override
    default Page<ModelEntity> getModels(ProjectEntity project,
                                        ModelType modelTypeFilter,
                                        Pageable pageable) {
        return findAllByProjectIdAndTypeEquals(project.getId(),
                                               modelTypeFilter.getName(),
                                               pageable);
    }

    @Override
    default Optional<ModelEntity> findModelById(String id) {
        return findById(id);
    }

    @Override
    default byte[] getModelContent(ModelEntity model) {
        return Optional.ofNullable(model.getContent())
                .map(String::getBytes)
                .orElse(new byte[0]);
    }

    @Override
    default byte[] getModelExport(ModelEntity model) {
        return getModelContent(model);
    }

    @Override
    default ModelEntity createModel(ModelEntity model) {
        return save(model);
    }

    @Override
    default ModelEntity updateModel(ModelEntity modelToBeUpdated,
                                    ModelEntity newModel) {
        modelToBeUpdated.setName(newModel.getName());
        modelToBeUpdated.setExtensions(newModel.getExtensions());
        return save(modelToBeUpdated);
    }

    @Override
    default ModelEntity updateModelContent(ModelEntity modelToBeUpdated,
                                           FileContent fileContent) {
        return save(modelToBeUpdated);
    }

    @Override
    default void deleteModel(ModelEntity model) {
        delete(model);
    }

    @Override
    default Class<ModelEntity> getModelType() {
        return ModelEntity.class;
    }
}
