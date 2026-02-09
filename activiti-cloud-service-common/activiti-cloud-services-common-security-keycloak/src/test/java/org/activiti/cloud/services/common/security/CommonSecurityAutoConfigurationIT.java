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

import org.activiti.cloud.services.common.security.config.CommonSecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CommonSecurityAutoConfigurationIT {

    @MockitoBean
    private BuildProperties buildProperties;

    @Autowired(required = false)
    private CommonSecurityAutoConfiguration commonSecurityAutoConfiguration;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {}

    @Test
    public void contextLoads() {
        assertThat(commonSecurityAutoConfiguration).isNotNull();
    }
}
