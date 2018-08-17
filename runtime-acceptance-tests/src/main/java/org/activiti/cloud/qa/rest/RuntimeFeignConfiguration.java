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

package org.activiti.cloud.qa.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Logger;
import feign.gson.GsonEncoder;
import feign.jackson.JacksonEncoder;
import org.activiti.cloud.api.model.shared.impl.conf.CloudCommonModelAutoConfiguration;
import org.activiti.cloud.api.process.model.impl.conf.CloudProcessModelAutoConfiguration;
import org.activiti.cloud.api.task.model.impl.conf.CloudTaskModelAutoConfiguration;
import org.activiti.cloud.qa.config.RuntimeTestsConfigurationProperties;
import org.activiti.cloud.qa.rest.feign.FeignConfiguration;
import org.activiti.cloud.qa.rest.feign.FeignErrorDecoder;
import org.activiti.cloud.qa.rest.feign.FeignRestDataClient;
import org.activiti.cloud.qa.rest.feign.HalDecoder;
import org.activiti.cloud.qa.rest.feign.OAuth2FeignRequestInterceptor;
import org.activiti.cloud.qa.service.AuditService;
import org.activiti.cloud.qa.service.QueryService;
import org.activiti.cloud.qa.service.RuntimeBundleDiagramService;
import org.activiti.cloud.qa.service.RuntimeBundleService;
import org.activiti.runtime.conf.CommonModelAutoConfiguration;
import org.activiti.runtime.conf.TaskModelAutoConfiguration;
import org.conf.activiti.runtime.ProcessModelAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Feign Configuration
 */
@Configuration
@Import({JacksonAutoConfiguration.class,
        FeignConfiguration.class,
        CloudCommonModelAutoConfiguration.class,
        CloudProcessModelAutoConfiguration.class,
        CloudTaskModelAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
        ProcessModelAutoConfiguration.class,
        TaskModelAutoConfiguration.class})
public class RuntimeFeignConfiguration {

    private final RuntimeTestsConfigurationProperties runtimeTestsConfigurationProperties;

    private final ObjectMapper objectMapper;

    public RuntimeFeignConfiguration(RuntimeTestsConfigurationProperties runtimeTestsConfigurationProperties,
                                     ObjectMapper objectMapper) {
        this.runtimeTestsConfigurationProperties = runtimeTestsConfigurationProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public RuntimeBundleService runtimeBundleService() {
        return FeignRestDataClient
                .builder()
                .encoder(new JacksonEncoder(objectMapper))
                .decoder(new HalDecoder(objectMapper))
                .target(RuntimeBundleService.class,
                        runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public RuntimeBundleDiagramService runtimeBundleDiagramService() {
        return Feign.builder()
                .encoder(new GsonEncoder())
                .errorDecoder(new FeignErrorDecoder())
                .logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.FULL)
                .requestInterceptor(new OAuth2FeignRequestInterceptor())
                .target(RuntimeBundleDiagramService.class,
                        runtimeTestsConfigurationProperties.getRuntimeBundleUrl());
    }

    @Bean
    public AuditService auditClient() {
        return FeignRestDataClient
                .builder()
                .encoder(new JacksonEncoder())
                .decoder(new HalDecoder(objectMapper))
                .target(AuditService.class,
                        runtimeTestsConfigurationProperties.getAuditEventUrl());
    }

    @Bean
    public QueryService queryService() {
        return FeignRestDataClient
                .builder()
                .encoder(new JacksonEncoder())
                .decoder(new HalDecoder(objectMapper))
                .target(QueryService.class,
                        runtimeTestsConfigurationProperties.getQueryUrl());
    }
}
