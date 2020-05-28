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

import java.util.Optional;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.entity.ModelVersionEntity;
import org.activiti.cloud.services.modeling.entity.ProjectEntity;
import org.activiti.cloud.services.modeling.jpa.version.VersionGenerationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class ModelRepositoryImpl implements ModelRepository<ProjectEntity, ModelEntity> {

    private final ModelJpaRepository modelJpaRepository;
    private final VersionGenerationHelper<ModelEntity, ModelVersionEntity> versionGenerationHelper;

    @Autowired
   public ModelRepositoryImpl(ModelJpaRepository modelJpaRepository){

       this.modelJpaRepository=modelJpaRepository;
        versionGenerationHelper = new VersionGenerationHelper<>(ModelEntity.class, ModelVersionEntity.class);
   }


    @Override
    public Page<ModelEntity> getModels(
        ProjectEntity project, ModelType modelTypeFilter,
        Pageable pageable) {
        return modelJpaRepository.findAllByProjectIdAndTypeEquals(project.getId(),
            modelTypeFilter.getName(),
            pageable);
    }

    @Override
    public boolean existsModelNameInProject(
        ProjectEntity project, String modelName, String modelTypeFilter) {
        return !modelJpaRepository.findModelByProjectIdAndNameEqualsAndTypeEquals(project != null ? project.getId() : null, modelName, modelTypeFilter)
            .isEmpty();
    }

    @Override
    public Optional<ModelEntity> findModelById(String modelId) {

        return modelJpaRepository.findById(modelId);
    }

    @Override
    public byte[] getModelContent(ModelEntity model) {

        return Optional.ofNullable(model.getContent())
            .orElse(new byte[0]);
    }

    @Override
    public byte[] getModelExport(ModelEntity model) {

        return getModelContent(model);
    }

    @Override
    public ModelEntity createModel(ModelEntity model) {

        model.setId(null);
        versionGenerationHelper.generateNextVersion(model);
        return modelJpaRepository.save(model);
    }

    @Override
    public ModelEntity updateModel(
        ModelEntity modelToBeUpdated, ModelEntity newModel) {
        Optional.ofNullable(newModel.getName())
            .ifPresent(modelToBeUpdated::setName);
        Optional.ofNullable(newModel.getExtensions())
            .ifPresent(modelToBeUpdated::setExtensions);
        versionGenerationHelper.generateNextVersion(modelToBeUpdated);
        return modelJpaRepository.save(modelToBeUpdated);
    }

    @Override
    public ModelEntity updateModelContent(
        ModelEntity modelToBeUpdate,
        FileContent fileContent) {

        return modelJpaRepository.save(modelToBeUpdate);
    }

    @Override
    public void deleteModel(ModelEntity model) {
        modelJpaRepository.delete(model);

    }

    @Override
    public Class<ModelEntity> getModelType() {
        return ModelEntity.class;
    }
}
