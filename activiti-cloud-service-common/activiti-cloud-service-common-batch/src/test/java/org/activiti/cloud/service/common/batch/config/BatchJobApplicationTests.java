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

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.service.common.batch.SpringBatchRestCoreTestApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Verifies that the Job Application starts with no jobs
 *
 */
@ExtendWith(OutputCaptureExtension.class)
public class BatchJobApplicationTests {

    @Test
    public void testBatchJobApp(CapturedOutput capturedOutput) throws Exception {
        // given
        final String JOB_RUN_MESSAGE = "Job1 was run";

        // when
        ConfigurableApplicationContext context = SpringApplication.run(SpringBatchRestCoreTestApplication.class);

        // then
        assertThat(capturedOutput.toString()).doesNotContain(JOB_RUN_MESSAGE);

        // and given
        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        Job job1 = context.getBean("job1", Job.class);

        // when
        jobLauncher.run(job1, new JobParameters());

        // then
        assertThat(capturedOutput.toString()).contains(JOB_RUN_MESSAGE);

    }

}