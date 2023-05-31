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

import java.util.List;
import java.util.Optional;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.process.ModelScope;
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
    public ModelRepositoryImpl(ModelJpaRepository modelJpaRepository) {
        this.modelJpaRepository = modelJpaRepository;
        versionGenerationHelper = new VersionGenerationHelper<>(ModelEntity.class, ModelVersionEntity.class);
    }

    @Override
    public Page<ModelEntity> getModels(ProjectEntity project, ModelType modelTypeFilter, Pageable pageable) {
        return modelJpaRepository.findAllByProjectIdAndTypeEquals(project.getId(), modelTypeFilter.getName(), pageable);
    }

    @Override
    public Page<ModelEntity> getModelsByName(ProjectEntity project, String name, Pageable pageable) {
        return modelJpaRepository.findAllByProjectIdAndNameLike(project.getId(), name, pageable);
    }

    @Override
    public Optional<ModelEntity> findModelByNameInProject(
        ProjectEntity project,
        String modelName,
        String modelTypeFilter
    ) {
        List<ModelEntity> models = modelJpaRepository.findModelByProjectIdAndNameEqualsAndTypeEquals(
            project != null ? project.getId() : null,
            modelName,
            modelTypeFilter
        );

        if (models != null && !models.isEmpty()) {
            return Optional.of(models.get(0));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ModelEntity> findGlobalModelByNameAndType(String modelName, String modelTypeFilter) {
        List<ModelEntity> models = modelJpaRepository.findModelByNameAndScopeAndTypeEquals(
            modelName,
            ModelScope.GLOBAL,
            modelTypeFilter
        );

        if (models != null && !models.isEmpty()) {
            return Optional.of(models.get(0));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ModelEntity> findModelById(String modelId) {
        return modelJpaRepository.findById(modelId);
    }

    @Override
    public byte[] getModelContent(ModelEntity model) {
        return Optional.ofNullable(model.getContent()).orElse(new byte[0]);
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
    public ModelEntity updateModel(ModelEntity modelToBeUpdated, ModelEntity newModel) {
        Optional.ofNullable(newModel.getName()).ifPresent(modelToBeUpdated::setName);
        Optional.ofNullable(newModel.getExtensions()).ifPresent(modelToBeUpdated::setExtensions);
        versionGenerationHelper.generateNextVersion(modelToBeUpdated);
        return modelJpaRepository.save(modelToBeUpdated);
    }

    public ModelEntity resetVersion(ModelEntity model) {
        model.getVersions().remove(model.getVersions().size() - 1);
        model.setLatestVersion(model.getVersions().get(0));
        return modelJpaRepository.save(model);
    }

    @Override
    public ModelEntity copyModel(ModelEntity model, ProjectEntity project) {
        ModelEntity modelEntityClone = new ModelEntity(model.getName(), model.getType());
        modelEntityClone.setExtensions(model.getExtensions());
        modelEntityClone.setContentType(model.getContentType());
        modelEntityClone.setContent(model.getContent());
        modelEntityClone.addProject(project);
        versionGenerationHelper.generateNextVersion(modelEntityClone);
        return modelJpaRepository.save(modelEntityClone);
    }

    @Override
    public ModelEntity updateModelContent(ModelEntity modelToBeUpdate, FileContent fileContent) {
        return modelJpaRepository.save(modelToBeUpdate);
    }

    @Override
    public void deleteModel(ModelEntity model) {
        modelJpaRepository.delete(model);
    }

    @Override
    public Page<ModelEntity> getGlobalModels(ModelType modelTypeFilter, boolean includeOrphans, Pageable pageable) {
        if (includeOrphans) {
            return modelJpaRepository.findAllByScopeAndTypeEqualsWithOrphans(
                ModelScope.GLOBAL,
                modelTypeFilter.getName(),
                pageable
            );
        } else {
            return modelJpaRepository.findAllByScopeAndTypeEquals(
                ModelScope.GLOBAL,
                modelTypeFilter.getName(),
                pageable
            );
        }
    }

    @Override
    public Class<ModelEntity> getModelType() {
        return ModelEntity.class;
    }
}
