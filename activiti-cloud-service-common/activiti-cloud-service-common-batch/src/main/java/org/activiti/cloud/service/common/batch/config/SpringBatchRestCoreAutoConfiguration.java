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

package org.activiti.cloud.service.common.batch.config;

import org.activiti.cloud.service.common.batch.controllers.JobController;
import org.activiti.cloud.service.common.batch.controllers.JobExecutionController;
import org.activiti.cloud.service.common.batch.controllers.JobResponseControllerAdvice;
import org.activiti.cloud.service.common.batch.core.JobBuilder;
import org.activiti.cloud.service.common.batch.core.JobStarter;
import org.activiti.cloud.service.common.batch.core.job.JobService;
import org.activiti.cloud.service.common.batch.core.jobexecution.JobExecutionService;
import org.activiti.cloud.service.common.batch.core.jobexecution.provider.DefaultJobExecutionProvider;
import org.activiti.cloud.service.common.batch.core.jobexecution.provider.JobExecutionProvider;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableBatchProcessing
@ConditionalOnProperty(name = SpringBatchRestCoreAutoConfiguration.REST_API_ENABLED,
                       havingValue = "true",
                       matchIfMissing = true)
public class SpringBatchRestCoreAutoConfiguration {

    public static final String REST_API_ENABLED = "org.activiti.cloud.service.common.batch.enabled";

    @Autowired(required = false)
    BuildProperties buildProperties;

    @Bean
    @ConditionalOnMissingBean
    public JobExecutionProvider jobExecutionProvider(JobExplorer jobExplorer) {
        return new DefaultJobExecutionProvider(jobExplorer);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobService jobService(JobRegistry jobRegistry) {
        return new JobService(jobRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobController jobController(JobService jobService) {
        return new JobController(jobService);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobExecutionController jobExecutionController(JobExecutionService jobExecutionService) {
        return new JobExecutionController(jobExecutionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobExecutionService jobExecutionService(JobExplorer jobExplorer,
                                                   JobExecutionProvider jobExecutionProvider,
                                                   JobStarter adHocStarter) {
        return new JobExecutionService(jobExplorer,
                                       jobExecutionProvider,
                                       adHocStarter);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobResponseControllerAdvice jobResponseControllerAdvice() {
        return new JobResponseControllerAdvice();
    }

    @Bean
    @ConditionalOnMissingBean
    public JobBuilder jobBuilder(JobRegistry jobRegistry,
                                 JobBuilderFactory jobs,
                                 StepBuilderFactory steps,
                                 Environment environment) {
        return new JobBuilder(jobRegistry,
                              jobs,
                              steps,
                              environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobStarter jobStarter(JobLocator jobLocator,
                      JobRepository jobRepository,
                      @Value("${org.activiti.cloud.service.common.batch.addUniqueJobParameter:true}")
                      boolean addUniqueJobParameter,
                      JobRegistry jobRegistry) {
        return new JobStarter(jobLocator,
                              jobRepository,
                              addUniqueJobParameter,
                              jobRegistry);
    }

}
