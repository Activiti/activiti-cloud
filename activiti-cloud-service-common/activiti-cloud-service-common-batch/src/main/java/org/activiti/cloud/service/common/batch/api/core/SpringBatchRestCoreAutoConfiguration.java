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

package org.activiti.cloud.service.common.batch.api.core;

import org.activiti.cloud.service.common.batch.api.core.job.JobController;
import org.activiti.cloud.service.common.batch.api.core.jobexecution.JobExecutionController;
import org.activiti.cloud.service.common.batch.util.core.JobStarter;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
@EnableBatchProcessing
@ConditionalOnProperty(name = SpringBatchRestCoreAutoConfiguration.REST_API_ENABLED,
        havingValue = "true",
        matchIfMissing = true)
@ComponentScan(basePackageClasses = {JobStarter.class, JobController.class, JobExecutionController.class})
public class SpringBatchRestCoreAutoConfiguration {

    public static final String REST_API_ENABLED = "org.activiti.cloud.service.common.batch.enabled";

    @Autowired(required = false)
    BuildProperties buildProperties;

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().components(new Components())
                            .info(new Info().title("Spring Batch REST")
                                            .version(buildProperties == null ? null : String.format("%s  -  Build time %s",
                                                                                                    buildProperties.getVersion(),
                                                                                                    buildProperties.getTime()))
                                            .description("REST API for controlling and viewing <a href=\"https://spring.io/projects/spring-batch\">" + "Spring Batch</a> jobs and their <a href=\"http://www.quartz-scheduler.org\">Quartz</a> schedules.")
                                            .license(new License().name("Apache License 2.0")
                                                                  .url("http://github.com/chrisgleissner/spring-batch-rest/blob/master/LICENSE")));
    }
}
