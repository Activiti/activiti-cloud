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
package org.activiti.cloud.acc.core.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign.Builder;
import feign.gson.GsonEncoder;
import feign.jackson.JacksonEncoder;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.conf.impl.ProcessModelAutoConfiguration;
import org.activiti.api.task.conf.impl.TaskModelAutoConfiguration;
import org.activiti.cloud.acc.core.config.RuntimeTestsConfigurationProperties;
import org.activiti.cloud.acc.core.rest.encoders.PageableQueryEncoder;
import org.activiti.cloud.acc.core.services.audit.AuditService;
import org.activiti.cloud.acc.core.services.audit.admin.AuditAdminService;
import org.activiti.cloud.acc.core.services.query.ApplicationQueryService;
import org.activiti.cloud.acc.core.services.query.ProcessModelQueryService;
import org.activiti.cloud.acc.core.services.query.ProcessQueryDiagramService;
import org.activiti.cloud.acc.core.services.query.ProcessQueryService;
import org.activiti.cloud.acc.core.services.query.TaskQueryService;
import org.activiti.cloud.acc.core.services.query.admin.ProcessModelQueryAdminService;
import org.activiti.cloud.acc.core.services.query.admin.ProcessQueryAdminDiagramService;
import org.activiti.cloud.acc.core.services.query.admin.ProcessQueryAdminService;
import org.activiti.cloud.acc.core.services.query.admin.TaskQueryAdminService;
import org.activiti.cloud.acc.core.services.runtime.ProcessVariablesRuntimeService;
import org.activiti.cloud.acc.core.services.runtime.admin.ProcessRuntimeAdminService;
import org.activiti.cloud.acc.core.services.runtime.admin.ProcessVariablesRuntimeAdminService;
import org.activiti.cloud.acc.core.services.runtime.admin.ServiceTasksAdminService;
import org.activiti.cloud.acc.core.services.runtime.admin.TaskRuntimeAdminService;
import org.activiti.cloud.acc.core.services.runtime.admin.TaskVariablesRuntimeAdminService;
import org.activiti.cloud.acc.core.services.runtime.diagram.ProcessRuntimeDiagramService;
import org.activiti.cloud.acc.shared.rest.feign.FeignConfiguration;
import org.activiti.cloud.acc.shared.rest.feign.FeignRestDataClient;
import org.activiti.cloud.acc.shared.rest.feign.HalDecoder;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.acc.shared.service.SwaggerService;
import org.activiti.cloud.api.model.shared.impl.conf.CloudCommonModelAutoConfiguration;
import org.activiti.cloud.api.process.model.impl.conf.CloudProcessModelAutoConfiguration;
import org.activiti.cloud.api.task.model.impl.conf.CloudTaskModelAutoConfiguration;
import org.activiti.cloud.services.rest.api.ProcessDefinitionsApiClient;
import org.activiti.cloud.services.rest.api.ProcessInstanceApiClient;
import org.activiti.cloud.services.rest.api.ProcessInstanceTasksApiClient;
import org.activiti.cloud.services.rest.api.TaskApiClient;
import org.activiti.cloud.services.rest.api.TaskVariableApiClient;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Feign Configuration
 */
@Configuration
@Import(
    {
        JacksonAutoConfiguration.class,
        FeignConfiguration.class,
        CloudCommonModelAutoConfiguration.class,
        CloudProcessModelAutoConfiguration.class,
        CloudTaskModelAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
        ProcessModelAutoConfiguration.class,
        TaskModelAutoConfiguration.class
    }
)
public class RuntimeFeignConfiguration {

    private final RuntimeTestsConfigurationProperties runtimeTestsConfigurationProperties;

    private final ObjectMapper objectMapper;

    public RuntimeFeignConfiguration(
        RuntimeTestsConfigurationProperties runtimeTestsConfigurationProperties,
        ObjectMapper objectMapper
    ) {
        this.runtimeTestsConfigurationProperties = runtimeTestsConfigurationProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public ProcessRuntimeDiagramService runtimeBundleDiagramService() {
        return baseFeignBuilder()
            .target(ProcessRuntimeDiagramService.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public TaskApiClient taskApiClient() {
        return FeignRestDataClient
            .builder(new PageableQueryEncoder(new JacksonEncoder(objectMapper)), new HalDecoder(objectMapper))
            .contract(new SpringMvcContract())
            .target(TaskApiClient.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public TaskVariableApiClient taskVariableApiClient() {
        return FeignRestDataClient
            .builder(new PageableQueryEncoder(new JacksonEncoder(objectMapper)), new HalDecoder(objectMapper))
            .contract(new SpringMvcContract())
            .target(TaskVariableApiClient.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public ProcessInstanceApiClient processInstanceApiClient() {
        return FeignRestDataClient
            .builder(new PageableQueryEncoder(new JacksonEncoder(objectMapper)), new HalDecoder(objectMapper))
            .contract(new SpringMvcContract())
            .target(ProcessInstanceApiClient.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public ProcessInstanceTasksApiClient processInstanceTasksApiClient() {
        return FeignRestDataClient
            .builder(new PageableQueryEncoder(new JacksonEncoder(objectMapper)), new HalDecoder(objectMapper))
            .contract(new SpringMvcContract())
            .target(ProcessInstanceTasksApiClient.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public ProcessDefinitionsApiClient processDefinitionsApiClient() {
        return FeignRestDataClient
            .builder(new PageableQueryEncoder(new JacksonEncoder(objectMapper)), new HalDecoder(objectMapper))
            .contract(new SpringMvcContract())
            .target(ProcessDefinitionsApiClient.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean(name = "runtimeBundleBaseService")
    public BaseService runtimeBundleBaseService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .contract(new SpringMvcContract())
            .target(BaseService.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean(name = "queryBaseService")
    public BaseService queryBaseService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .contract(new SpringMvcContract())
            .target(BaseService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    @Bean(name = "auditBaseService")
    public BaseService auditBaseService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .contract(new SpringMvcContract())
            .target(BaseService.class, runtimeTestsConfigurationProperties.getAuditEventUrl());
    }

    @Bean
    public ProcessRuntimeAdminService processRuntimeAdminService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .target(ProcessRuntimeAdminService.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public TaskRuntimeAdminService taskRuntimeAdminService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .target(TaskRuntimeAdminService.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public ServiceTasksAdminService serviceTasksAdminService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .target(ServiceTasksAdminService.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public ProcessQueryService processQueryService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(), new HalDecoder(objectMapper))
            .target(ProcessQueryService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    @Bean
    public TaskQueryService taskQueryService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(), new HalDecoder(objectMapper))
            .target(TaskQueryService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    @Bean
    public ProcessQueryAdminService processQueryAdminService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(), new HalDecoder(objectMapper))
            .target(ProcessQueryAdminService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    @Bean
    public TaskQueryAdminService taskQueryAdminService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(), new HalDecoder(objectMapper))
            .target(TaskQueryAdminService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    @Bean
    public AuditService auditClient() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(), new HalDecoder(objectMapper))
            .target(AuditService.class, runtimeTestsConfigurationProperties.getAuditEventUrl());
    }

    @Bean
    public AuditAdminService auditAdminClient() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(), new HalDecoder(objectMapper))
            .target(AuditAdminService.class, runtimeTestsConfigurationProperties.getAuditEventUrl());
    }

    @Bean
    public ProcessVariablesRuntimeAdminService processVariablesRuntimeAdminService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .target(
                ProcessVariablesRuntimeAdminService.class,
                runtimeTestsConfigurationProperties.getRuntimeBundleUrl()
            );
    }

    @Bean
    public ProcessVariablesRuntimeService processVariablesRuntimeService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .target(ProcessVariablesRuntimeService.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public ProcessModelQueryService processModelQueryService() {
        return FeignRestDataClient
            .builder(new feign.codec.Encoder.Default(), new feign.codec.Decoder.Default())
            .target(ProcessModelQueryService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    @Bean
    public ProcessModelQueryAdminService processModelQueryAdminService() {
        return FeignRestDataClient
            .builder(new feign.codec.Encoder.Default(), new feign.codec.Decoder.Default())
            .target(ProcessModelQueryAdminService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    @Bean
    public TaskVariablesRuntimeAdminService taskVariablesRuntimeAdminService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .target(TaskVariablesRuntimeAdminService.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public SwaggerService runtimeBundleSwaggerService() {
        return FeignRestDataClient
            .builder(new feign.codec.Encoder.Default(), new feign.codec.Decoder.Default())
            .target(SwaggerService.class, runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public SwaggerService querySwaggerService() {
        return FeignRestDataClient
            .builder(new feign.codec.Encoder.Default(), new feign.codec.Decoder.Default())
            .target(SwaggerService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    @Bean
    public SwaggerService auditSwaggerService() {
        return FeignRestDataClient
            .builder(new feign.codec.Encoder.Default(), new feign.codec.Decoder.Default())
            .target(SwaggerService.class, runtimeTestsConfigurationProperties.getAuditEventUrl());
    }

    @Bean
    public ProcessQueryDiagramService queryDiagramService() {
        return baseFeignBuilder()
            .target(ProcessQueryDiagramService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    private Builder baseFeignBuilder() {
        return FeignRestDataClient.builder(new GsonEncoder(), new feign.codec.Decoder.Default());
    }

    @Bean
    public ProcessQueryAdminDiagramService queryAdminDiagramService() {
        return baseFeignBuilder()
            .target(ProcessQueryAdminDiagramService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }

    @Bean
    public ApplicationQueryService applicationQueryService() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(), new HalDecoder(objectMapper))
            .target(ApplicationQueryService.class, runtimeTestsConfigurationProperties.getQueryUrl());
    }
}
