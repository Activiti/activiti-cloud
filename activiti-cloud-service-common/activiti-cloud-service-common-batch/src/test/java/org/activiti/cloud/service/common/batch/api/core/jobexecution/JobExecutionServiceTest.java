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

import static java.lang.Integer.MAX_VALUE;
import static java.util.Optional.empty;
import static org.activiti.cloud.service.common.batch.api.core.Fixtures.JOB_NAME_1;
import static org.activiti.cloud.service.common.batch.api.core.Fixtures.JOB_NAME_2;
import static org.activiti.cloud.service.common.batch.api.core.Fixtures.configureForJobExecutionsService;
import static org.activiti.cloud.service.common.batch.api.core.Fixtures.configureMock;
import static org.activiti.cloud.service.common.batch.api.core.Fixtures.je11;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Optional;

import javax.batch.operations.NoSuchJobExecutionException;

import org.activiti.cloud.service.common.batch.api.core.jobexecution.provider.DefaultJobExecutionProvider;
import org.activiti.cloud.service.common.batch.util.core.JobStarter;
import org.activiti.cloud.service.common.batch.util.core.JobConfig;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.explore.JobExplorer;

@RunWith(MockitoJUnitRunner.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JobExecutionServiceTest {

    @Mock
    private JobExplorer jobExplorer;
    @Mock
    private JobStarter adHocStarter;
    private DefaultJobExecutionProvider jobExecutionProvider;
    private JobExecutionService jobExecutionService;

    @Before
    public void setUp() {
        configureMock(jobExplorer);
        jobExecutionProvider = new DefaultJobExecutionProvider(jobExplorer);
        configureMock(adHocStarter);

        configureForJobExecutionsService(jobExplorer);
        when(jobExplorer.getJobExecution(je11.getId())).thenReturn(je11);

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
        JobExecution je = jobExecutionService.jobExecution(je11.getId());
        assertThat(je).isNotNull();
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void jobExecutionsIdNotFound() {
        jobExecutionService.jobExecution(10);
    }

    @Test(expected = NoSuchJobExecutionException.class)
    public void jobExecutionsIdNotFoundNegativeId() {
        jobExecutionService.jobExecution(-1);
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
        Assertions.assertThat(jes).extracting(je -> je.getJobName()).containsExactly(JOB_NAME_2, JOB_NAME_1);
    }
}