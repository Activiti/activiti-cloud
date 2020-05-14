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
package org.activiti.cloud.qa.rest;

import feign.gson.GsonEncoder;
import org.activiti.cloud.acc.shared.rest.feign.FeignConfiguration;
import org.activiti.cloud.acc.shared.rest.feign.FeignRestDataClient;
import org.activiti.cloud.acc.shared.rest.feign.HalDecoder;
import org.activiti.cloud.qa.config.AppsTestsConfigurationProperties;
import org.activiti.cloud.qa.service.AppsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Feign Configuration
 */
@Import(FeignConfiguration.class)
@Configuration
public class AppsFeignConfiguration {

    @Autowired
    private AppsTestsConfigurationProperties appsTestsConfigurationProperties;


    @Bean
    AppsService appsService(){
        return FeignRestDataClient
                .builder(new GsonEncoder(),
                         new HalDecoder())
                .target(AppsService.class,
                        appsTestsConfigurationProperties.getAppsUrl());
    }
}
