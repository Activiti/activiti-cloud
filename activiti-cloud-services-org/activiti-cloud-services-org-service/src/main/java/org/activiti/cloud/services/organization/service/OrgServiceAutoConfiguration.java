package org.activiti.cloud.services.organization.service;

import java.util.Map;
import java.util.Set;

import org.activiti.cloud.organization.api.ContentUpdateListener;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelContent;
import org.activiti.cloud.organization.api.ModelContentConverter;
import org.activiti.cloud.organization.api.ModelContentValidator;
import org.activiti.cloud.organization.api.ModelExtensionsValidator;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.converter.JsonConverter;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.service.api.ProjectService;
import org.activiti.cloud.services.organization.validation.extensions.ExtensionsModelValidator;
import org.activiti.cloud.services.organization.validation.project.ProjectValidator;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrgServiceAutoConfiguration {

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
                                     JsonConverter<Model> jsonConverter) {
        return new ModelService(modelRepository,
                                modelTypeService,
                                modelContentService,
                                modelExtensionsService,
                                jsonConverter);

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
                                         JsonConverter<Map> jsonMetadataConverter,
                                         Set<ProjectValidator> projectValidators) {

        return new ProjectServiceImpl(projectRepository,
                                  modelService,
                                  modelTypeService,
                                  jsonConverter,
                                  jsonMetadataConverter,
                                  projectValidators);

    }

}
