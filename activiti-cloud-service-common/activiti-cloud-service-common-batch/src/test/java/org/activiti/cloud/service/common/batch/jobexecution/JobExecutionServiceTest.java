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

package org.activiti.cloud.service.common.batch.jobexecution;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Optional;

import javax.batch.operations.NoSuchJobExecutionException;

import org.activiti.cloud.service.common.batch.Fixtures;
import org.activiti.cloud.service.common.batch.core.JobStarter;
import org.activiti.cloud.service.common.batch.core.jobexecution.JobExecutionService;
import org.activiti.cloud.service.common.batch.core.jobexecution.provider.DefaultJobExecutionProvider;
import org.activiti.cloud.service.common.batch.domain.JobConfig;
import org.activiti.cloud.service.common.batch.domain.JobExecution;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.explore.JobExplorer;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JobExecutionServiceTest {

    @Mock
    private JobExplorer jobExplorer;
    @Mock
    private JobStarter adHocStarter;
    private DefaultJobExecutionProvider jobExecutionProvider;
    private JobExecutionService jobExecutionService;

    @BeforeEach
    public void setUp() {
        Fixtures.configureMock(jobExplorer);
        jobExecutionProvider = new DefaultJobExecutionProvider(jobExplorer);
        Fixtures.configureMock(adHocStarter);

        Fixtures.configureForJobExecutionsService(jobExplorer);
        when(jobExplorer.getJobExecution(Fixtures.je11.getId())).thenReturn(Fixtures.je11);

        jobExecutionService = new JobExecutionService(jobExplorer, jobExecutionProvider, adHocStarter);
    }

    @Test
    public void launchJob() {
        JobExecution jobExecution = jobExecutionService.launch(JobConfig.builder().name("j1").build());
        assertThat(jobExecution.getJobName()).matches("j1");
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.STARTED);
    }

    @Test
    public void jobExecutionsAll() {
        Collection<JobExecution> jes = jobExecutionService.jobExecutions(empty(), empty(), MAX_VALUE);
        Assertions.assertThat(jes).hasSize(6);
    }

    @Test
    public void jobExecutionsId() {
        JobExecution je = jobExecutionService.jobExecution(Fixtures.je11.getId());
        assertThat(je).isNotNull();
    }

    @Test
    public void jobExecutionsIdNotFound() {
        Assertions.assertThatExceptionOfType(NoSuchJobExecutionException.class).isThrownBy(() -> {
            jobExecutionService.jobExecution(10);
        });
    }

    @Test
    public void jobExecutionsIdNotFoundNegativeId() {
        Assertions.assertThatExceptionOfType(NoSuchJobExecutionException.class).isThrownBy(() -> {
            jobExecutionService.jobExecution(-1);
        });
    }

    @Test
    public void jobExecutionsJobNameRegexp() {
        Collection<JobExecution> jes = jobExecutionService.jobExecutions(Optional.of("j1"), empty(), MAX_VALUE);
        Assertions.assertThat(jes).hasSize(2);
    }

    @Test
    public void jobExecutionsStatus() {
        Collection<JobExecution> jes = jobExecutionService.jobExecutions(Optional.of("j1"),
                                                                         Optional.of(ExitStatus.COMPLETED.getExitCode()),
                                                                         MAX_VALUE);
        Assertions.assertThat(jes).hasSize(1);
    }

    @Test
    public void jobExecutionsMaxNumberOfJobInstancesFailed() {
        Collection<JobExecution> jes = jobExecutionService.jobExecutions(empty(),
                                                                         Optional.of(ExitStatus.FAILED.getExitCode()),
                                                                         1);
        Assertions.assertThat(jes).hasSize(2);
        Assertions.assertThat(jes).extracting(je -> je.getExitCode()).allMatch(s -> s.equals("FAILED"));
    }

    @Test
    public void jobExecutionsMaxNumberOfJobInstancesCompleted() {
        Collection<JobExecution> jes = jobExecutionService.jobExecutions(empty(),
                                                                         Optional.of(ExitStatus.COMPLETED.getExitCode()),
                                                                         1);
        Assertions.assertThat(jes).hasSize(2);
        Assertions.assertThat(jes).extracting(je -> je.getJobName()).containsExactly(Fixtures.JOB_NAME_2, Fixtures.JOB_NAME_1);
    }
}