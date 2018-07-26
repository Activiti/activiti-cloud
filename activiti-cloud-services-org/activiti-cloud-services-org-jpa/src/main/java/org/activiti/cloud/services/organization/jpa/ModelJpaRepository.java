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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.organization.entity.ApplicationEntity;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.activiti.cloud.services.organization.entity.ModelEntityHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import static org.activiti.cloud.services.organization.entity.ModelEntityHandler.createModelReference;
import static org.activiti.cloud.services.organization.entity.ModelEntityHandler.deleteModelReference;
import static org.activiti.cloud.services.organization.entity.ModelEntityHandler.loadModelReference;
import static org.activiti.cloud.services.organization.entity.ModelEntityHandler.updateModelReference;
import static org.activiti.cloud.services.organization.entity.ModelEntityHandler.validateModelReference;

/**
 * JPA Repository for {@link ModelEntity} entity
 */
@RepositoryRestResource(path = "models",
        collectionResourceRel = "models",
        itemResourceRel = "models",
        exported = false)
public interface ModelJpaRepository extends JpaRepository<ModelEntity, String>,
                                            ModelRepository<ApplicationEntity, ModelEntity> {

    Page<ModelEntity> findAllByApplicationIdIsNull(Pageable pageable);

    Page<ModelEntity> findAllByApplicationId(String applicationId,
                                             Pageable pageable);

    @Override
    default Page<ModelEntity> getTopLevelModels(Pageable pageable) {
        return loadModelReference(findAllByApplicationIdIsNull(pageable));
    }

    @Override
    default Page<ModelEntity> getModels(ApplicationEntity application,
                                        Pageable pageable) {
        return loadModelReference(findAllByApplicationId(application.getId(),
                                                         pageable));
    }

    @Override
    default Optional<ModelEntity> findModelById(String id) {
        return findById(id).map(ModelEntityHandler::loadModelReference);
    }

    @Override
    default ModelEntity createModel(ModelEntity model) {
        if (model.getId() == null) {
            model.setId(UUID.randomUUID().toString());
        }
        createModelReference(model);
        return loadModelReference(save(model));
    }

    @Override
    default ModelEntity updateModel(ModelEntity model) {
        updateModelReference(model);
        return loadModelReference(save(model));
    }

    @Override
    default void deleteModel(ModelEntity model) {
        deleteModelReference(model);
        delete(model);
    }

    @Override
    default List<ModelValidationError> validateModelContent(ModelEntity model,
                                                            byte[] content) {
        return validateModelReference(model,
                                    content);
    }

    @Override
    default Class<ModelEntity> getModelType() {
        return ModelEntity.class;
    }
}
