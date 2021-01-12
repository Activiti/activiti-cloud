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

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.batch.core.ExitStatus.COMPLETED;
import static org.springframework.batch.core.ExitStatus.FAILED;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.activiti.cloud.service.common.batch.api.core.jobexecution.provider.JobExecutionProvider;
import org.activiti.cloud.service.common.batch.util.core.JobStarter;
import org.activiti.cloud.service.common.batch.util.core.JobConfig;
import org.mockito.ArgumentMatchers;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;

public class Fixtures {

    private static final List<String> JOB_NAMES = newArrayList("j1", "j2");

    public static final String JOB_NAME_1 = "j1";
    public static final JobInstance ji11 = new JobInstance(11L, JOB_NAME_1);
    public static final JobExecution je11 = jobExecution(11, ji11, COMPLETED);
    public static final JobInstance ji12 = new JobInstance(12L, JOB_NAME_1);
    public static final JobExecution je12 = jobExecution(12, ji12, FAILED);

    public static final String JOB_NAME_2 = "j2";
    public static final JobInstance ji21 = new JobInstance(21L, JOB_NAME_2);
    public static final JobExecution je21 = jobExecution(21, ji21, COMPLETED);
    public static final JobInstance ji22 = new JobInstance(22L, JOB_NAME_2);
    public static final JobExecution je22 = jobExecution(22, ji22, COMPLETED);
    public static final JobInstance ji23 = new JobInstance(23L, JOB_NAME_2);
    public static final JobExecution je23 = jobExecution(23, ji23, FAILED);
    public static final JobExecution je24 = jobExecution(24, ji23, FAILED); // Re-run of job instance 23

    public static void configureMock(JobExplorer jobExplorer) {
        reset(jobExplorer);
        when(jobExplorer.getJobNames()).thenReturn(JOB_NAMES);
    }

    public static void configureMock(JobRegistry jobRegistry) {
        reset(jobRegistry);
        when(jobRegistry.getJobNames()).thenReturn(JOB_NAMES);
    }

    public static void configureMock(JobStarter adHocStarter) {
        reset(adHocStarter);
        when(adHocStarter.start(isA(JobConfig.class))).thenReturn(jobExecution(1, ji11, ExitStatus.EXECUTING));
    }

    public static void configureForJobExecutionsService(JobExplorer jobExplorer) {
        when(jobExplorer.getJobInstances(eq(JOB_NAME_1), anyInt(), anyInt())).thenReturn(newArrayList(ji11, ji12));
        when(jobExplorer.getJobInstances(eq(JOB_NAME_2), anyInt(), anyInt())).thenReturn(newArrayList(ji21,
                                                                                                      ji22,
                                                                                                      ji23));

        when(jobExplorer.getJobExecutions(ji11)).thenReturn(newArrayList(je11));
        when(jobExplorer.getJobExecutions(ji12)).thenReturn(newArrayList(je12));

        when(jobExplorer.getJobExecutions(ji21)).thenReturn(newArrayList(je21));
        when(jobExplorer.getJobExecutions(ji22)).thenReturn(newArrayList(je22));
        when(jobExplorer.getJobExecutions(ji23)).thenReturn(newArrayList(je23, je24));
    }

//    public static void configureForJobExecutionsService(CachedJobExecutionProvider provider) {
//        provider.accept(je11);
//        provider.accept(je12);
//
//        provider.accept(je21);
//        provider.accept(je22);
//        provider.accept(je23);
//        provider.accept(je24);
//    }

    public static void configureForJobExecutionsService(JobExecutionProvider provider) {
        when(provider.getJobExecutions(ArgumentMatchers.<Optional<String>>any(),
                                       ArgumentMatchers.<Optional<String>>any(),
                                       anyInt())).thenReturn(newArrayList(je11,
                                                                          je12,
                                                                          je21,
                                                                          je22,
                                                                          je23,
                                                                          je24));
    }

    public static JobExecution jobExecution(int id, JobInstance ji, ExitStatus exitStatus) {
        JobExecution jobExecution = new JobExecution(ji, (long) id, null, "config" + id);
        jobExecution.setCreateTime(new Date(id * 100L));
        jobExecution.setStartTime(new Date(id * 200L));

        if (!exitStatus.getExitCode().equals(ExitStatus.EXECUTING.getExitCode())) {
            jobExecution.setEndTime(new Date(id * 300L));
            if (exitStatus.getExitCode().equals(ExitStatus.FAILED.getExitCode()))
                jobExecution.setStatus(BatchStatus.FAILED);
            else
                jobExecution.setStatus(BatchStatus.COMPLETED);
        } else
            jobExecution.setStatus(BatchStatus.STARTED);

        jobExecution.setExitStatus(exitStatus);
        return jobExecution;
    }
}
