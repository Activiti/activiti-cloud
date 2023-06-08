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
package org.activiti.cloud.services.modeling.liquibase;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.activiti.cloud.common.liquibase.SpringLiquibaseConfigurationSupport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", matchIfMissing = true)
@PropertySource("classpath:config/modeling-liquibase.properties")
public class ActivitiCloudModelingLiquibaseAutoConfiguration extends SpringLiquibaseConfigurationSupport {

    @Bean
    @ConditionalOnMissingBean(name = "modelingLiquibase")
    public SpringLiquibase modelingLiquibase(DataSource dataSource, LiquibaseProperties modelingLiquibaseProperties) {
        return buildSpringLiquibase(dataSource, modelingLiquibaseProperties);
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.liquibase")
    public LiquibaseProperties modelingLiquibaseProperties() {
        return new LiquibaseProperties();
    }
}
