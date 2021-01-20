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

package org.activiti.cloud.services.query.batch;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.activiti.cloud.services.query.batch.config.CleanupQueryProcessInstancesJobConfiguration;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Verifies that the Job Application starts with no jobs
 *
 */
@ExtendWith(OutputCaptureExtension.class)
public class BatchJobApplicationTests {

    @SpringBootApplication
    @EntityScan(basePackageClasses =  ProcessInstanceEntity.class)
    static class TestApplication {

        @Value("classpath:data.sql")
        private Resource dataSql;

        @Bean
        public ApplicationRunner initData(DataSource dataSource) {
            return args -> ScriptUtils.executeSqlScript(dataSource.getConnection(),
                                                        dataSql);
        }
    }

    @Test
    public void testCleanupQueryProcessInstancesHistoryJob(CapturedOutput capturedOutput) throws Exception {
        // given
        final String JOB_RUN_MESSAGE = "Completed process instance history cleanup";

        // when
        ConfigurableApplicationContext application = new SpringApplicationBuilder().properties("debug=true")
                                                                                   .sources(TestApplication.class)
                                                                                   .run();


        // then
        assertThat(capturedOutput.toString()).doesNotContain(JOB_RUN_MESSAGE);

        // and given
        JobLauncher jobLauncher = application.getBean(JobLauncher.class);
        Job job = application.getBean(CleanupQueryProcessInstancesJobConfiguration.JOB_NAME, Job.class);

        // when
        jobLauncher.run(job, new JobParameters());

        // then
        assertThat(capturedOutput.toString()).contains(JOB_RUN_MESSAGE);
    }
}