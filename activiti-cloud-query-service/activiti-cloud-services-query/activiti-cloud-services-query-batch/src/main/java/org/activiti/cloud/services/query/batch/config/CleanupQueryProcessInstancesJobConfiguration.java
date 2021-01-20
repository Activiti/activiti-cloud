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

package org.activiti.cloud.services.query.batch.config;

import javax.persistence.EntityManager;

import org.activiti.cloud.services.query.batch.CleanupQueryProcessInstanceHistoryTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CleanupQueryProcessInstancesJobConfiguration {

    public static final String JOB_NAME = "cleanupQueryProcessInstancesHistoryJob";

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    @ConditionalOnMissingBean(name = JOB_NAME)
    public Job cleanupQueryProcessInstancesHistoryJob(EntityManager entityManager) {
        return this.jobBuilderFactory.get(JOB_NAME)
            .start(this.stepBuilderFactory.get(JOB_NAME + ".tasklet")
                .tasklet(new CleanupQueryProcessInstanceHistoryTasklet(entityManager))
                .build())
            .build();
    }
}