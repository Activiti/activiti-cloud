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
package org.activiti.cloud.starter.rb.configuration;

import java.util.ArrayList;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.core.io.ClassPathResource;

public class ResourceQueryConfigurer implements ProcessEngineConfigurationConfigurer {

    static final String RESOURCE_MAPPER_PATH = "org/activiti/cloud/starter/rb/db/mapping/entity/Resource.xml";

    @Override
    public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
        if (processEngineConfiguration.getCustomMybatisXMLMappers() == null) {
            processEngineConfiguration.setCustomMybatisXMLMappers(new ArrayList<>());
        }

        processEngineConfiguration
            .getCustomMybatisXMLMappers()
            .add(new ClassPathResource(RESOURCE_MAPPER_PATH));
    }
}
