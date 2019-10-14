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

package org.activiti.cloud.services.rest.conf;

import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.rest.assemblers.ConnectorDefinitionResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionMetaResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceVariableResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.TaskResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.TaskVariableInstanceResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.ToCloudProcessDefinitionConverter;
import org.activiti.cloud.services.rest.assemblers.ToCloudProcessInstanceConverter;
import org.activiti.cloud.services.rest.assemblers.ToCloudTaskConverter;
import org.activiti.cloud.services.rest.assemblers.ToCloudVariableInstanceConverter;
import org.activiti.cloud.services.rest.controllers.ProcessVariablesPayloadValidator;
import org.activiti.cloud.services.rest.controllers.ResourcesAssembler;
import org.activiti.cloud.services.rest.controllers.RuntimeBundleRelProvider;
import org.activiti.cloud.services.rest.controllers.TaskVariablesPayloadDateHandler;
import org.activiti.common.util.DateFormatterProvider;
import org.activiti.spring.process.model.ProcessExtensionModel;
import org.activiti.spring.process.variable.VariableValidationService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

@Configuration
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class ServicesRestWebMvcAutoConfiguration implements WebMvcConfigurer {

    private final Jackson2ObjectMapperBuilder objectMapperBuilder;

    public ServicesRestWebMvcAutoConfiguration(Jackson2ObjectMapperBuilder objectMapperBuilder) {
        this.objectMapperBuilder = objectMapperBuilder;
    }

    @Bean
    public ResourcesAssembler resourcesAssembler() {
        return new ResourcesAssembler();
    }
    
    @Bean
    public ConnectorDefinitionResourceAssembler connectorDefinitionResourceAssembler() {
        return new ConnectorDefinitionResourceAssembler();
    }

    @Bean
    public ProcessDefinitionMetaResourceAssembler processDefinitionMetaResourceAssembler() {
        return new ProcessDefinitionMetaResourceAssembler();
    }
    
    @Bean
    public ProcessInstanceResourceAssembler processInstanceResourceAssembler(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ProcessInstanceResourceAssembler(new ToCloudProcessInstanceConverter(runtimeBundleInfoAppender));
    }

    @Bean
    public ProcessDefinitionResourceAssembler processDefinitionResourceAssembler(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ProcessDefinitionResourceAssembler(new ToCloudProcessDefinitionConverter(runtimeBundleInfoAppender));
    }

    @Bean
    public TaskResourceAssembler taskResourceAssembler(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new TaskResourceAssembler(new ToCloudTaskConverter(runtimeBundleInfoAppender));
    }

    @Bean
    public ToCloudVariableInstanceConverter cloudVariableInstanceConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ToCloudVariableInstanceConverter(runtimeBundleInfoAppender);
    }

    @Bean
    public ProcessInstanceVariableResourceAssembler processInstanceVariableResourceAssembler(ToCloudVariableInstanceConverter converter) {
        return new ProcessInstanceVariableResourceAssembler(converter);
    }

    @Bean
    public TaskVariableInstanceResourceAssembler taskVariableInstanceResourceAssembler(ToCloudVariableInstanceConverter converter) {
        return new TaskVariableInstanceResourceAssembler(converter);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessVariablesPayloadValidator processVariablesPayloadValidator(DateFormatterProvider dateFormatterProvider,
                                                                             Map<String, ProcessExtensionModel> processExtensionModelMap,
                                                                             VariableValidationService variableValidationService) {
        return new ProcessVariablesPayloadValidator(dateFormatterProvider,
                                                    processExtensionModelMap,
                                                    variableValidationService);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TaskVariablesPayloadDateHandler taskVariablesPayloadValidator(DateFormatterProvider dateFormatterProvider) {
        return new TaskVariablesPayloadDateHandler(dateFormatterProvider);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // for some, not yet identified, reason the ObjectMapper used by MappingJackson2HttpMessageConverter
        // does not contains the object mapper customizations provided by custom Module beans.
        // need to call configure here to ensure that the customisations are registered
        for (HttpMessageConverter<?> converter : converters) {
            //should exclude TypeConstrainedMappingJackson2HttpMessageConverter from configuration
            if (converter instanceof MappingJackson2HttpMessageConverter && !(converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter)) {
                objectMapperBuilder.configure(((MappingJackson2HttpMessageConverter) converter).getObjectMapper());
            }
        }

    }
    
    @Bean
    @ConditionalOnMissingBean
    public RuntimeBundleRelProvider runtimeBundleRelProvider() {
        return new RuntimeBundleRelProvider();
    }
}
