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
package org.activiti.cloud.qa.story.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.jackson.JacksonEncoder;
import org.activiti.cloud.acc.shared.rest.feign.EnableFeignContext;
import org.activiti.cloud.acc.shared.rest.feign.FeignRestDataClient;
import org.activiti.cloud.acc.shared.rest.feign.HalDecoder;
import org.activiti.cloud.qa.story.client.IdentityManagementClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@EnableFeignContext
public class IdentityManagementConfiguration {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdentityManagementTestsConfigurationProperties properties;

    @Bean
    public IdentityManagementClient identityManagementClient() {
        return FeignRestDataClient
            .builder(new JacksonEncoder(objectMapper), new HalDecoder(objectMapper))
            .contract(new SpringMvcContract())
            .target(IdentityManagementClient.class, properties.getIdentityManagementUrl());
    }
}
