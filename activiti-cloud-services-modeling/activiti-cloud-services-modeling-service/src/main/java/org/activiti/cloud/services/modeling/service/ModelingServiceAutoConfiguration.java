package org.activiti.cloud.services.modeling.service;

import java.util.Map;
import java.util.Set;

import org.activiti.cloud.modeling.api.ContentUpdateListener;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContent;
import org.activiti.cloud.modeling.api.ModelContentConverter;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelExtensionsValidator;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.activiti.cloud.services.modeling.service.api.ModelService;
import org.activiti.cloud.services.modeling.service.api.ProjectService;
import org.activiti.cloud.services.modeling.validation.extensions.ExtensionsModelValidator;
import org.activiti.cloud.services.modeling.validation.project.ProjectValidator;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelingServiceAutoConfiguration {

    @Bean
    public ModelContentService modelContentService(Set<ModelContentValidator> modelValidators,
                                                   Set<ModelContentConverter<? extends ModelContent>> modelConverters,
                                                   Set<ContentUpdateListener> contentUpdateListeners) {
        return new ModelContentService(modelValidators,
                                       modelConverters,
                                       contentUpdateListeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExtensionsModelValidator extensionsModelValidator(SchemaLoader modelExtensionsSchemaLoader) {
        return new ExtensionsModelValidator(modelExtensionsSchemaLoader);
    }


    @Bean
    public ModelExtensionsService modelExtensionsService(Set<ModelExtensionsValidator> metadataValidators,
                                                         ExtensionsModelValidator extensionsModelValidator,
                                                         ModelTypeService modelTypeService) {
        return new ModelExtensionsService(metadataValidators,
                                          extensionsModelValidator,
                                          modelTypeService);
    }

    @Bean
    public ModelService modelService(ModelRepository modelRepository,
                                     ModelTypeService modelTypeService,
                                     ModelContentService modelContentService,
                                     ModelExtensionsService modelExtensionsService,
                                     JsonConverter<Model> jsonConverter,
                                     ProcessModelContentConverter processModelContentConverter) {
        return new ModelServiceImpl(modelRepository,
                                    modelTypeService,
                                    modelContentService,
                                    modelExtensionsService,
                                    jsonConverter,
                                    processModelContentConverter);

    }

    @Bean
    public ModelTypeService modelTypeService(Set<ModelType> availableModelTypes) {
        return new ModelTypeService(availableModelTypes);
    }

    @Bean
    public ProjectService projectService(ProjectRepository projectRepository,
                                         ModelService modelService,
                                         ModelTypeService modelTypeService,
                                         JsonConverter<Project> jsonConverter,
                                         JsonConverter<ProjectDescriptor> projectDescriptorJsonConverter,
                                         JsonConverter<Map> jsonMetadataConverter,
                                         Set<ProjectValidator> projectValidators) {

        return new ProjectServiceImpl(projectRepository,
                                      modelService,
                                      modelTypeService,
                                      projectDescriptorJsonConverter,
                                      jsonConverter,
                                      jsonMetadataConverter,
                                      projectValidators);

    }

}
