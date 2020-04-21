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
        this.modelJpaRepository.delete(model);

    }

    @Override
    public Class<ModelEntity> getModelType() {
        return ModelEntity.class;
    }
}
