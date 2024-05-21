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
package org.activiti.cloud.starter.rb.configuration;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import org.activiti.api.process.model.payloads.CreateProcessInstancePayload;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.cloud.common.swagger.springdoc.BaseOpenApiBuilder;
import org.activiti.cloud.common.swagger.springdoc.SwaggerDocUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;

@Configuration
public class RuntimeBundleSwaggerConfig implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeBundleSwaggerConfig.class.getName());

    @Bean
    @ConditionalOnMissingBean(name = "runtimeBundleApi")
    public GroupedOpenApi runtimeBundleApi(@Value("${activiti.cloud.swagger.rb-base-path:}") String swaggerBasePath) {
        return GroupedOpenApi
            .builder()
            .group("Runtime Bundle")
            .packagesToScan("org.activiti.cloud.services.rest")
            .addOpenApiCustomizer(openApi ->
                openApi.addExtension(BaseOpenApiBuilder.SERVICE_URL_PREFIX, swaggerBasePath)
            )
            .addOpenApiCustomizer(openApiCustomizer())
            .build();
    }

    public OpenApiCustomizer openApiCustomizer() {
        return openAPI ->
            openAPI
                .getPaths()
                .values()
                .stream()
                .flatMap(val -> val.readOperations().stream())
                .flatMap(operation -> operation.getResponses().entrySet().stream())
                .filter(entry -> entry.getKey().matches(String.valueOf(HttpStatus.OK.value())))
                .forEach(entry -> {
                    LOGGER.info(entry.toString());
                    Content contents = entry.getValue().getContent();
                    String applicationHal = MediaTypes.HAL_JSON_VALUE;
                    String applicationJson = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
                    if (
                        contents != null &&
                        contents.containsKey(applicationHal) &&
                        contents.containsKey(applicationJson)
                    ) {
                        LOGGER.info("-------------------------------");
                        LOGGER.info(contents.toString());
                        MediaType applicationHalValue = contents.remove(applicationHal);
                        contents.put(applicationHal, applicationHalValue);
                        LOGGER.info("------------------------------");
                        LOGGER.info(contents.toString());
                    }
                });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SwaggerDocUtils.replaceWithClass(StartProcessPayload.class, PayloadApiModels.StartProcessPayloadApiModel.class);
        SwaggerDocUtils.replaceWithClass(SignalPayload.class, PayloadApiModels.SignalPayloadApiModel.class);
        SwaggerDocUtils.replaceWithClass(
            UpdateProcessPayload.class,
            PayloadApiModels.UpdateProcessPayloadApiModel.class
        );
        SwaggerDocUtils.replaceWithClass(
            SetProcessVariablesPayload.class,
            PayloadApiModels.SetProcessVariablesPayloadApiModel.class
        );
        SwaggerDocUtils.replaceWithClass(
            RemoveProcessVariablesPayload.class,
            PayloadApiModels.RemoveProcessVariablesPayloadApiModel.class
        );
        SwaggerDocUtils.replaceWithClass(AssignTaskPayload.class, PayloadApiModels.AssignTaskPayloadApiModel.class);
        SwaggerDocUtils.replaceWithClass(CompleteTaskPayload.class, PayloadApiModels.CompleteTaskPayloadApiModel.class);
        SwaggerDocUtils.replaceWithClass(
            CandidateGroupsPayload.class,
            PayloadApiModels.CandidateGroupsPayloadApiModel.class
        );
        SwaggerDocUtils.replaceWithClass(
            CandidateUsersPayload.class,
            PayloadApiModels.CandidateUsersPayloadApiModel.class
        );
        SwaggerDocUtils.replaceWithClass(CreateTaskPayload.class, PayloadApiModels.CreateTaskPayloadApiModel.class);
        SwaggerDocUtils.replaceWithClass(
            CreateTaskVariablePayload.class,
            PayloadApiModels.CreateTaskVariablePayloadApiModel.class
        );
        SwaggerDocUtils.replaceWithClass(
            UpdateTaskVariablePayload.class,
            PayloadApiModels.UpdateTaskVariablePayloadApiModel.class
        );
        SwaggerDocUtils.replaceWithClass(UpdateTaskPayload.class, PayloadApiModels.UpdateTaskPayloadApiModel.class);
        SwaggerDocUtils.replaceWithClass(SaveTaskPayload.class, PayloadApiModels.SaveTaskPayloadApiModel.class);
        SwaggerDocUtils.replaceWithClass(
            CreateProcessInstancePayload.class,
            PayloadApiModels.CreateProcessInstancePayloadApiModel.class
        );
    }
}
