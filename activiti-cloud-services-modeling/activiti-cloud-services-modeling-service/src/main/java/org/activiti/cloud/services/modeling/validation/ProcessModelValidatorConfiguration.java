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
package org.activiti.cloud.services.modeling.validation;

import java.util.Set;

import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.process.Extensions;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.services.modeling.converter.ConnectorModelContentConverter;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.activiti.cloud.services.modeling.validation.extensions.ProcessExtensionsModelValidator;
import org.activiti.cloud.services.modeling.validation.extensions.ProcessExtensionsProcessVariablesValidator;
import org.activiti.cloud.services.modeling.validation.extensions.ProcessExtensionsTaskMappingsValidator;
import org.activiti.cloud.services.modeling.validation.extensions.ProcessExtensionsValidator;
import org.activiti.cloud.services.modeling.validation.extensions.TaskMappingsServiceTaskImplementationValidator;
import org.activiti.cloud.services.modeling.validation.extensions.TaskMappingsValidator;
import org.activiti.cloud.services.modeling.validation.process.BpmnModelCallActivityValidator;
import org.activiti.cloud.services.modeling.validation.process.BpmnModelEngineValidator;
import org.activiti.cloud.services.modeling.validation.process.BpmnModelNameValidator;
import org.activiti.cloud.services.modeling.validation.process.BpmnModelServiceTaskImplementationValidator;
import org.activiti.cloud.services.modeling.validation.process.BpmnModelUserTaskAssigneeValidator;
import org.activiti.cloud.services.modeling.validation.process.BpmnModelValidator;
import org.activiti.cloud.services.modeling.validation.process.ProcessModelValidator;
import org.activiti.cloud.services.modeling.validation.project.ProjectConsistencyValidator;
import org.activiti.cloud.services.modeling.validation.project.ProjectNameValidator;
import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ProcessValidatorImpl;
import org.activiti.validation.validator.ValidatorSetFactory;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public ProcessExtensionsModelValidator processExtensionsModelValidator(SchemaLoader processExtensionsSchemaLoader,
                                                                           Set<ProcessExtensionsValidator> processExtensionsValidators,
                                                                           ProcessModelType processModelType,
                                                                           JsonConverter<Extensions> jsonExtensionsConverter,
                                                                           ProcessModelContentConverter processModelContentConverter) {
        return new ProcessExtensionsModelValidator(processExtensionsSchemaLoader,
                                                   processExtensionsValidators,
                                                   processModelType,
                                                   jsonExtensionsConverter,
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
