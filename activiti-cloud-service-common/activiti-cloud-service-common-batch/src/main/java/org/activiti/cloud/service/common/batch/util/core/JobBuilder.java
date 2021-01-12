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

import java.util.function.Consumer;

import org.activiti.cloud.service.common.batch.util.core.tasklet.PropertyResolverConsumerTasklet;
import org.activiti.cloud.service.common.batch.util.core.tasklet.RunnableTasklet;
import org.activiti.cloud.service.common.batch.util.core.tasklet.StepExecutionListenerTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component @RequiredArgsConstructor
public class JobBuilder {
    private final JobRegistry jobRegistry;
    private final JobBuilderFactory jobs;
    private final StepBuilderFactory steps;
    private final Environment environment;

    public static Job registerJob(JobRegistry jobRegistry, Job job) {
        jobRegistry.unregister(job.getName());
        try {
            jobRegistry.register(new JobFactory() {
                @Override
                public Job createJob() {
                    return job;
                }

                @Override
                public String getJobName() {
                    return job.getName();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Could not create " + job.getName(), e);
        }
        return job;
    }

    public Job registerJob(Job job) {
        return registerJob(jobRegistry, job);
    }

    public Job createJob(String name, Runnable runnable) {
        return createJob(name, new RunnableTasklet(runnable));
    }

    private Job createJob(String name, Tasklet tasklet) {
        return registerJob(jobs.get(name).incrementer(new RunIdIncrementer())
                .start(steps.get("step").allowStartIfComplete(true).tasklet(tasklet).build()).build());
    }

    public Job createJob(String name, Consumer<PropertyResolver> propertyResolverConsumer) {
        return createJob(name, new PropertyResolverConsumerTasklet(environment, propertyResolverConsumer));
    }

    public Job createJobFromStepExecutionConsumer(String name, Consumer<StepExecution> stepExecutionConsumer) {
        return createJob(name, new StepExecutionListenerTasklet(stepExecutionConsumer));
    }
}
