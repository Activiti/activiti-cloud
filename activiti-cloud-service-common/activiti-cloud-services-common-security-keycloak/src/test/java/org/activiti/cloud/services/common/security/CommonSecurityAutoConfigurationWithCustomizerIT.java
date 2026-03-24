/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.activiti.cloud.services.common.security.config.CommonSecurityAutoConfiguration;
import org.activiti.cloud.services.common.security.config.HttpSecurityCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(CommonSecurityAutoConfigurationWithCustomizerIT.CustomizerConfig.class)
class CommonSecurityAutoConfigurationWithCustomizerIT {

    @MockitoBean
    private BuildProperties buildProperties;

    @MockitoBean
    private HttpSecurityCustomizer httpSecurityCustomizer;

    @Autowired(required = false)
    private CommonSecurityAutoConfiguration commonSecurityAutoConfiguration;

    @Test
    void shouldApplyHttpSecurityCustomizers_whenPresent() throws Exception {
        assertThat(commonSecurityAutoConfiguration).isNotNull();

        verify(httpSecurityCustomizer).customize(any(HttpSecurity.class));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {}

    @TestConfiguration
    static class CustomizerConfig {

        @Bean
        HttpSecurityCustomizer testHttpSecurityCustomizer(HttpSecurityCustomizer httpSecurityCustomizer) {
            return httpSecurityCustomizer;
        }
    }
}
