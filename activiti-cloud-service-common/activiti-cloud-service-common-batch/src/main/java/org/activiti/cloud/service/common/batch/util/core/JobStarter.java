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

package org.activiti.cloud.service.common.batch.util.core;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.batch.operations.BatchRuntimeException;

import org.activiti.cloud.service.common.batch.util.JobParamUtil;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobStarter {

    private final JobLocator jobLocator;
    private final SimpleJobLauncher asyncJobLauncher;
    private final SimpleJobLauncher syncJobLauncher;
    private final boolean addUniqueJobParameter;
    private final JobRegistry jobRegistry;

    public JobStarter(JobLocator jobLocator,
                        JobRepository jobRepository,
                        @Value("${org.activiti.cloud.service.common.batch.api.core.addUniqueJobParameter:true}") boolean addUniqueJobParameter,
                        JobRegistry jobRegistry) {
        this.jobLocator = jobLocator;
        asyncJobLauncher = jobLauncher(new SimpleAsyncTaskExecutor(), jobRepository);
        syncJobLauncher = jobLauncher(new SyncTaskExecutor(), jobRepository);
        this.addUniqueJobParameter = addUniqueJobParameter;
        this.jobRegistry = jobRegistry;
        log.info("Adding unique job parameter: {}", addUniqueJobParameter);
    }

    private SimpleJobLauncher jobLauncher(TaskExecutor taskExecutor, JobRepository jobRepository) {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(taskExecutor);
        return jobLauncher;
    }

    public JobExecution start(Job job) {
        return this.start(job, true, null);
    }

    public JobExecution start(Job job, Boolean async, Map<String, Object> properties) {
        Job existingJob = null;
        try {
            existingJob = jobRegistry.getJob(job.getName());
        } catch (NoSuchJobException e) {
            log.info("Registering new job: " + job.getName());
        }
        JobConfig jobConfig = JobConfig.builder()
                                       .asynchronous(async)
                                       .properties(properties == null ? new HashMap<>() : properties)
                                       .name(job.getName())
                                       .build();
        JobBuilder.registerJob(jobRegistry, existingJob == null ? job : existingJob);
        return this.start(jobConfig);
    }

    public JobExecution start(JobConfig jobConfig) {
        try {
            Job job = jobLocator.getJob(jobConfig.getName());

            // TODO resolve properties from environment
            Map<String, JobParameter> params = JobParamUtil.convertRawToParamMap(jobConfig.getProperties());

            if (addUniqueJobParameter)
                params.put("uuid", new JobParameter(UUID.randomUUID().toString()));

            JobParameters jobParameters = new JobParameters(params);

            log.info("Starting {} with {}", jobConfig.getName(), jobConfig);

            JobLauncher jobLauncher = jobConfig.isAsynchronous() ? asyncJobLauncher : syncJobLauncher;

            return jobLauncher.run(job, jobParameters);
        } catch (JobExecutionException e) {
            throw new BatchRuntimeException(format("Failed to start job '%s' with %s. Reason: %s",
                                                   jobConfig.getName(),
                                                   jobConfig,
                                                   e.getMessage()), e);
        } catch (Exception e) {
            throw new RuntimeException(format("Failed to start job '%s' with %s. Reason: %s",
                                              jobConfig.getName(),
                                              jobConfig,
                                              e.getMessage()), e);
        }
    }
}
