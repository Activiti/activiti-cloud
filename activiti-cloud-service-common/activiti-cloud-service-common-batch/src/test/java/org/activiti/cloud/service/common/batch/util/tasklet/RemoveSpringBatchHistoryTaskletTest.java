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

package org.activiti.cloud.service.common.batch.util.tasklet;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@SpringBootTest
public class RemoveSpringBatchHistoryTaskletTest {

    @Autowired
    private RemoveSpringBatchHistoryTasklet removeSpringBatchHistoryTasklet;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private DataSource dataSource;

    @SpringBootApplication
    @TestConfiguration
    static class TaskletConfiguration {

        @Bean
        public RemoveSpringBatchHistoryTasklet removeSpringBatchHistoryTasklet(JdbcTemplate jdbcTemplate) {
            return new RemoveSpringBatchHistoryTasklet(jdbcTemplate);
        }
    }

    @BeforeEach
    public void beforeEach() throws ScriptException, SQLException {
        // setup
        Resource sqlScript = new ClassPathResource("RemoveSpringBatchHistoryTaskletTest.sql");
        ScriptUtils.executeSqlScript(dataSource.getConnection(), sqlScript);
    }

    @Test
    public void execute() {
        // given
        List<JobInstance> jobInstances = jobExplorer.getJobInstances("jobTest", 0, 5);
        assertThat(jobInstances.size()).describedAs("2 job instances before the purge")
                                       .isEqualTo(2);

        // when
        final ChunkContext chunkContext = new ChunkContext(null);
        StepExecution stepExecution = new StepExecution("step1", null);
        StepContribution stepContribution = new StepContribution(stepExecution);
        removeSpringBatchHistoryTasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(stepContribution.getWriteCount()).describedAs("6 lines should be deleted from the history")
                                                    .isEqualTo(6);

        jobInstances = jobExplorer.getJobInstances("jobTest", 0, 5);
        assertThat(jobInstances.size()).describedAs("Just a single job instance after the delete")
                                .isEqualTo(1);

        JobInstance jobInstance = jobInstances.get(0);
        assertThat(jobInstance.getId()).describedAs("Only the job instance number 2 should remain into the history")
                                       .isEqualTo(Long.valueOf(102));
    }
}