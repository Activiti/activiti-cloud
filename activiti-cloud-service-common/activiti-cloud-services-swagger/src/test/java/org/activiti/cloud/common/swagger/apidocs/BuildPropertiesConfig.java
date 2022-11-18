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
package org.activiti.cloud.common.swagger.apidocs;

import java.util.Properties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class BuildPropertiesConfig {

    @Bean
    BuildProperties buildProperties() {
        Properties entries = new Properties();
        entries.setProperty("version", "0.0.1");
        entries.setProperty("inceptionYear", "1900");
        entries.setProperty("year", "1992");
        entries.setProperty("organization.name", "Alfresco");
        entries.setProperty("organization.url", "alfresco.com");
        return new BuildProperties(entries);
    }
}
