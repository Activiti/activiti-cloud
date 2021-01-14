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

package org.activiti.cloud.service.common.batch.jobexecution.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.core.ExitStatus.COMPLETED;
import static org.springframework.batch.core.ExitStatus.FAILED;

import java.util.Collection;
import java.util.Optional;

import org.activiti.cloud.service.common.batch.Fixtures;
import org.activiti.cloud.service.common.batch.core.jobexecution.provider.JobExecutionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.JobExecution;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class AbstractJobExecutionProviderTest {

    public static final int MAX_NUMBER_OF_EXECUTIONS_PER_JOB_NAME = 5;

    protected abstract JobExecutionProvider provider();

    @Test
    public void worksForEmptyOptionals() {
        Collection<JobExecution> jes = provider().getJobExecutions(Optional.empty(), Optional.empty(), MAX_NUMBER_OF_EXECUTIONS_PER_JOB_NAME);
        assertThat(jes).containsExactly(Fixtures.je24, Fixtures.je23, Fixtures.je22, Fixtures.je21, Fixtures.je12, Fixtures.je11);
    }

    @Test
    public void worksForCompleted() {
        Collection<JobExecution> jes = provider().getJobExecutions(Optional.of(Fixtures.JOB_NAME_2), Optional.of(COMPLETED.getExitCode()), MAX_NUMBER_OF_EXECUTIONS_PER_JOB_NAME);
        assertThat(jes).containsExactly(Fixtures.je22, Fixtures.je21);
    }

    @Test
    public void limitsReturnedValuesForCompleted() {
        Collection<JobExecution> jes = provider().getJobExecutions(Optional.of(Fixtures.JOB_NAME_2), Optional.of(COMPLETED.getExitCode()), 1);
        assertThat(jes).containsExactly(Fixtures.je22);
    }

    @Test
    public void worksForFailed() {
        Collection<JobExecution> jes = provider().getJobExecutions(Optional.of(Fixtures.JOB_NAME_1), Optional.of(FAILED.getExitCode()), MAX_NUMBER_OF_EXECUTIONS_PER_JOB_NAME);
        assertThat(jes).containsExactly(Fixtures.je12);
    }

    @Test
    public void limitsReturnedValuesForFailed() {
        Collection<JobExecution> jes = provider().getJobExecutions(Optional.of(Fixtures.JOB_NAME_2), Optional.of(FAILED.getExitCode()), 1);
        assertThat(jes).containsExactly(Fixtures.je24);
    }

    @Test
    public void sortsResultsInDescendingDateOrder() {
        Collection<JobExecution> jes = provider().getJobExecutions(Optional.of(Fixtures.JOB_NAME_2), Optional.of(FAILED.getExitCode()), MAX_NUMBER_OF_EXECUTIONS_PER_JOB_NAME);
        assertThat(jes).containsExactly(Fixtures.je24, Fixtures.je23);
    }

}
