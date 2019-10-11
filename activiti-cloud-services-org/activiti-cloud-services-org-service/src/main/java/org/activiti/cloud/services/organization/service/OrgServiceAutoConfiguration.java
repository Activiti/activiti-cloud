package org.activiti.cloud.services.organization.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelContent;
import org.activiti.cloud.organization.api.ModelContentConverter;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidator;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.converter.JsonConverter;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.validation.project.ProjectValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class OrgServiceAutoConfiguration {

    @Bean
    public ModelContentService modelContentService(ModelTypeService modelTypeService,
                                                   Set<ModelValidator> modelValidators,
                                                   Set<ModelContentConverter<? extends ModelContent>> modelConverters) {
        return new ModelContentService(modelTypeService,
                                       modelValidators,
                                       modelConverters);
    }

    @Bean
    public ModelService modelService(ModelRepository modelRepository,
                                     ModelTypeService modelTypeService,
                                     ModelContentService modelContentService,
                                     JsonConverter<Model> jsonConverter,
                                     ObjectMapper objectMapper) {
        return new ModelService(modelRepository,
                                modelTypeService,
                                modelContentService,
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
                                         Set<ProjectValidator> projectValidators) {

        return new ProjectService(projectRepository,
                                  modelService,
                                  modelTypeService,
                                  jsonConverter,
                                  projectValidators);

    }

}
