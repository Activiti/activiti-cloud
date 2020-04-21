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

import java.util.List;

import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.rest.assemblers.ConnectorDefinitionRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.GroupCandidatesRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionMetaRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceVariableRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.TaskRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.TaskVariableInstanceRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ToCandidateGroupConverter;
import org.activiti.cloud.services.rest.assemblers.ToCandidateUserConverter;
import org.activiti.cloud.services.rest.assemblers.ToCloudProcessDefinitionConverter;
import org.activiti.cloud.services.rest.assemblers.ToCloudProcessInstanceConverter;
import org.activiti.cloud.services.rest.assemblers.ToCloudTaskConverter;
import org.activiti.cloud.services.rest.assemblers.ToCloudVariableInstanceConverter;
import org.activiti.cloud.services.rest.assemblers.UserCandidatesRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.controllers.RuntimeBundleLinkRelationProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class ServicesRestWebMvcAutoConfiguration implements WebMvcConfigurer {

    private final Jackson2ObjectMapperBuilder objectMapperBuilder;

    public ServicesRestWebMvcAutoConfiguration(Jackson2ObjectMapperBuilder objectMapperBuilder) {
        this.objectMapperBuilder = objectMapperBuilder;
    }

    @Bean
    public CollectionModelAssembler resourcesAssembler() {
        return new CollectionModelAssembler();
    }

    @Bean
    public ConnectorDefinitionRepresentationModelAssembler connectorDefinitionRepresentationModelAssembler() {
        return new ConnectorDefinitionRepresentationModelAssembler();
    }

    @Bean
    public ProcessDefinitionMetaRepresentationModelAssembler processDefinitionMetaRepresentationModelAssembler() {
        return new ProcessDefinitionMetaRepresentationModelAssembler();
    }

    @Bean
    public ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ProcessInstanceRepresentationModelAssembler(new ToCloudProcessInstanceConverter(runtimeBundleInfoAppender));
    }

    @Bean
    public ProcessDefinitionRepresentationModelAssembler processDefinitionRepresentationModelAssembler(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ProcessDefinitionRepresentationModelAssembler(new ToCloudProcessDefinitionConverter(runtimeBundleInfoAppender));
    }

    @Bean
    public TaskRepresentationModelAssembler taskRepresentationModelAssembler(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new TaskRepresentationModelAssembler(new ToCloudTaskConverter(runtimeBundleInfoAppender));
    }

    @Bean
    public UserCandidatesRepresentationModelAssembler userCandidatesRepresentationModelAssembler(){
        return new UserCandidatesRepresentationModelAssembler();
    }

    @Bean
    public GroupCandidatesRepresentationModelAssembler groupCandidatesRepresentationModelAssembler(){
        return new GroupCandidatesRepresentationModelAssembler();
    }

    @Bean
    public ToCandidateUserConverter toCandidateUsersConverter(){
        return new ToCandidateUserConverter();
    }

    @Bean
    public ToCandidateGroupConverter toCandidateGroupsConverter(){
        return new ToCandidateGroupConverter();
    }

    @Bean
    public ToCloudVariableInstanceConverter cloudVariableInstanceConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new ToCloudVariableInstanceConverter(runtimeBundleInfoAppender);
    }

    @Bean
    public ProcessInstanceVariableRepresentationModelAssembler processInstanceVariableRepresentationModelAssembler(ToCloudVariableInstanceConverter converter) {
        return new ProcessInstanceVariableRepresentationModelAssembler(converter);
    }

    @Bean
    public TaskVariableInstanceRepresentationModelAssembler taskVariableInstanceRepresentationModelAssembler(ToCloudVariableInstanceConverter converter) {
        return new TaskVariableInstanceRepresentationModelAssembler(converter);
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
    public RuntimeBundleLinkRelationProvider runtimeBundleRelProvider() {
        return new RuntimeBundleLinkRelationProvider();
    }
}
