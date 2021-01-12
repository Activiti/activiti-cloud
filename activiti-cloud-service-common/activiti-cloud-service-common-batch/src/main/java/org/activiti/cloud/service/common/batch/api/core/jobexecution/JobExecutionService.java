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

package org.activiti.cloud.service.common.batch.api.core.jobexecution;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.batch.operations.NoSuchJobExecutionException;

import org.activiti.cloud.service.common.batch.api.core.job.Job;
import org.activiti.cloud.service.common.batch.api.core.jobexecution.provider.JobExecutionProvider;
import org.activiti.cloud.service.common.batch.util.core.JobStarter;
import org.activiti.cloud.service.common.batch.util.core.JobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobExecutionService {

    private final static Logger logger = LoggerFactory.getLogger(JobExecutionService.class);
    private final JobStarter adHocStarter;
    private final JobExplorer jobExplorer;
    private final JobExecutionProvider jobExecutionProvider;

    @Autowired
    public JobExecutionService(JobExplorer jobExplorer,
                               JobExecutionProvider jobExecutionProvider,
                               JobStarter adHocStarter) {
        this.jobExplorer = jobExplorer;
        this.adHocStarter = adHocStarter;
        this.jobExecutionProvider = jobExecutionProvider;
    }

    public JobExecution jobExecution(long executionId) {
        org.springframework.batch.core.JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        if (jobExecution == null)
            throw new NoSuchJobExecutionException("Could not find job execution with ID " + executionId);
        return JobExecution.fromSpring(jobExecution);

    }

    public Collection<JobExecution> jobExecutions(Optional<String> jobNameRegexp,
                                                  Optional<String> exitCode,
                                                  int maxNumberOfExecutionsPerJobName) {
        logger.debug("Getting job executions(jobNameRegexp={}, exitCode={}, maxNumberOfExecutionsPerJobName={})",
                     jobNameRegexp,
                     exitCode,
                     maxNumberOfExecutionsPerJobName);
        return jobExecutionProvider.getJobExecutions(jobNameRegexp, exitCode, maxNumberOfExecutionsPerJobName)
                                   .stream()
                                   .map(JobExecution::fromSpring)
                                   .collect(Collectors.toList());

    }

    public JobExecution launch(JobConfig jobConfig) {
        return JobExecution.fromSpring(adHocStarter.start(jobConfig));
    }

    public Job job(String jobName) {
        return new Job(jobName);
    }
}
