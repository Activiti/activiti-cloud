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
package org.activiti.cloud.acc.shared.rest.feign;

import feign.Feign;
import feign.Logger;
import feign.form.FormEncoder;
import feign.gson.GsonDecoder;
import org.activiti.cloud.acc.shared.config.BaseTestsConfigurationProperties;
import org.activiti.cloud.acc.shared.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

/**
 * Feign Configuration
 */

public class FeignConfiguration {

    @Autowired
    private BaseTestsConfigurationProperties baseTestsConfigurationProperties;

    @Bean
    public AuthenticationService authenticationClient() {
        return Feign
            .builder()
            .encoder(new FormEncoder())
            .decoder(new GsonDecoder())
            .logger(new Logger.ErrorLogger())
            .logLevel(Logger.Level.FULL)
            .target(AuthenticationService.class, baseTestsConfigurationProperties.getAuthUrl());
    }
}
