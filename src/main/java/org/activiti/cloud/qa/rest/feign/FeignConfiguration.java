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

package org.activiti.cloud.qa.rest.feign;

import feign.Feign;
import feign.Logger;
import feign.form.FormEncoder;
import feign.gson.GsonDecoder;
import org.activiti.cloud.qa.config.TestsConfigurationProperties;
import org.activiti.cloud.qa.service.AuditService;
import org.activiti.cloud.qa.service.AuthenticationService;
import org.activiti.cloud.qa.service.ModelingGroupsService;
import org.activiti.cloud.qa.service.ModelingModelsService;
import org.activiti.cloud.qa.service.ModelingProjectsService;
import org.activiti.cloud.qa.service.QueryService;
import org.activiti.cloud.qa.service.RuntimeBundleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign Configuration
 */
@Configuration
public class FeignConfiguration {

    @Autowired
    private TestsConfigurationProperties acceptanceTestsConfiguration;

    @Bean
    public AuthenticationService authenticationClient() {
        return Feign.builder()
                .encoder(new FormEncoder())
                .decoder(new GsonDecoder())
                .logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.FULL)
                .target(AuthenticationService.class,
                        acceptanceTestsConfiguration.getAuthUrl());
    }

    @Bean
    public RuntimeBundleService runtimeBundleService() {
        return FeignRestDataClient
                .builder()
                .target(RuntimeBundleService.class,
                        acceptanceTestsConfiguration.getRuntimeBundleUrl());
    }

    @Bean
    public AuditService auditClient() {
        return FeignRestDataClient
                .builder()
                .target(AuditService.class,
                        acceptanceTestsConfiguration.getAuditEventUrl());
    }

    @Bean
    public ModelingGroupsService modelingGroupsService() {
        return ModelingGroupsService
                .build(acceptanceTestsConfiguration.getModelingUrl());
    }

    @Bean
    public ModelingProjectsService modelingProjectsService() {
        return ModelingProjectsService
                .build(acceptanceTestsConfiguration.getModelingUrl());
    }

    @Bean
    public ModelingModelsService modelingModelsService() {
        return ModelingModelsService
                .build(acceptanceTestsConfiguration.getModelingUrl());
    }

    @Bean
    public QueryService queryService() {
        return FeignRestDataClient
                .builder()
                .target(QueryService.class,
                        acceptanceTestsConfiguration.getQueryUrl());
    }
}
