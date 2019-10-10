/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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
package org.activiti.cloud.services.organization.validation;

import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.converter.JsonConverter;
import org.activiti.cloud.services.organization.converter.ConnectorModelContentConverter;
import org.activiti.cloud.services.organization.converter.ProcessModelContentConverter;
import org.activiti.cloud.services.organization.validation.extensions.ExtensionsModelValidator;
import org.activiti.cloud.services.organization.validation.extensions.ProcessExtensionsProcessVariablesValidator;
import org.activiti.cloud.services.organization.validation.extensions.ProcessExtensionsTaskMappingsValidator;
import org.activiti.cloud.services.organization.validation.extensions.ProcessExtensionsValidator;
import org.activiti.cloud.services.organization.validation.extensions.TaskMappingsServiceTaskImplementationValidator;
import org.activiti.cloud.services.organization.validation.extensions.TaskMappingsValidator;
import org.activiti.cloud.services.organization.validation.process.BpmnModelCallActivityValidator;
import org.activiti.cloud.services.organization.validation.process.BpmnModelEngineValidator;
import org.activiti.cloud.services.organization.validation.process.BpmnModelNameValidator;
import org.activiti.cloud.services.organization.validation.process.BpmnModelServiceTaskImplementationValidator;
import org.activiti.cloud.services.organization.validation.process.BpmnModelUserTaskAssigneeValidator;
import org.activiti.cloud.services.organization.validation.process.BpmnModelValidator;
import org.activiti.cloud.services.organization.validation.process.ProcessModelValidator;
import org.activiti.cloud.services.organization.validation.project.ProjectConsistencyValidator;
import org.activiti.cloud.services.organization.validation.project.ProjectNameValidator;
import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ProcessValidatorImpl;
import org.activiti.validation.validator.ValidatorSetFactory;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Configuration for process model validator
 */
@Configuration
public class ProcessModelValidatorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProcessValidatorImpl processValidator() {
        ProcessValidatorImpl processValidator = new ProcessValidatorImpl();
        processValidator.addValidatorSet(new ValidatorSetFactory().createActivitiExecutableProcessValidatorSet());
        return processValidator;
    }

    @Bean
    @ConditionalOnMissingBean
    public ExtensionsModelValidator extensionsModelValidator(SchemaLoader processExtensionsSchemaLoader,
                                        Set<ProcessExtensionsValidator> processExtensionsValidators,
                                        ProcessModelType processModelType,
                                        JsonConverter<Model> extensionsConverter,
                                        ProcessModelContentConverter processModelContentConverter) {
        return new ExtensionsModelValidator(processExtensionsSchemaLoader,
                                            processExtensionsValidators,
                                            processModelType,
                                            extensionsConverter,
                                            processModelContentConverter);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessExtensionsProcessVariablesValidator processExtensionsProcessVariablesValidator() {
        return new ProcessExtensionsProcessVariablesValidator();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessExtensionsTaskMappingsValidator processExtensionsTaskMappingsValidator(Set<TaskMappingsValidator> taskMappingsValidators) {
        return new ProcessExtensionsTaskMappingsValidator(taskMappingsValidators);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TaskMappingsServiceTaskImplementationValidator taskMappingsServiceTaskImplementationValidator(ConnectorModelType connectorModelType,
                                                                                                         ConnectorModelContentConverter connectorModelContentConverter) {
        return new TaskMappingsServiceTaskImplementationValidator(connectorModelType,
                                                                  connectorModelContentConverter);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProjectConsistencyValidator projectConsistencyValidator(ProcessModelType processModelType) {
        return new ProjectConsistencyValidator(processModelType);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProjectNameValidator ProjectNameValidator() {
        return new ProjectNameValidator();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ConnectorModelValidator connectorModelValidator(SchemaLoader connectorSchemaLoader,
                                                           ConnectorModelType connectorModelType) {
        return new ConnectorModelValidator(connectorSchemaLoader,
                                           connectorModelType);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public BpmnModelCallActivityValidator bpmnModelCallActivityValidator(ProcessModelType processModelType,
                                                                         ProcessModelContentConverter processModelContentConverter) {
        return new BpmnModelCallActivityValidator(processModelType,
                                                  processModelContentConverter);

    }
    
    @Bean
    @ConditionalOnMissingBean
    public BpmnModelEngineValidator bpmnModelEngineValidator(ProcessValidator processValidator) {
        return new BpmnModelEngineValidator(processValidator);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public BpmnModelNameValidator bpmnModelNameValidator() {
        return new BpmnModelNameValidator();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public BpmnModelServiceTaskImplementationValidator bpmnModelServiceTaskImplementationValidator(ConnectorModelType connectorModelType,
                                                                                                   ConnectorModelContentConverter connectorModelContentConverter) {
        return new BpmnModelServiceTaskImplementationValidator(connectorModelType,
                                                               connectorModelContentConverter);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public BpmnModelUserTaskAssigneeValidator bpmnModelUserTaskAssigneeValidator() {
        return new BpmnModelUserTaskAssigneeValidator();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessModelValidator processModelValidator(ProcessModelType processModelType,
                                                       Set<BpmnModelValidator> mpmnModelValidators,
                                                       ProcessModelContentConverter processModelContentConverter) {
        return new ProcessModelValidator(processModelType,
                                         mpmnModelValidators,
                                         processModelContentConverter);

    }
}
