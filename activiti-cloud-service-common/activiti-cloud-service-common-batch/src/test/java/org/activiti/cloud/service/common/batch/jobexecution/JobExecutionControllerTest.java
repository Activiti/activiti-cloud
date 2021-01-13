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

import static org.activiti.cloud.service.common.batch.Fixtures.configureForJobExecutionsService;
import static org.activiti.cloud.service.common.batch.Fixtures.configureMock;
import static org.activiti.cloud.service.common.batch.Fixtures.je11;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.batch.operations.BatchRuntimeException;

import org.activiti.cloud.service.common.batch.controllers.JobExecutionController;
import org.activiti.cloud.service.common.batch.core.JobStarter;
import org.activiti.cloud.service.common.batch.core.jobexecution.JobExecutionService;
import org.activiti.cloud.service.common.batch.core.jobexecution.provider.DefaultJobExecutionProvider;
import org.activiti.cloud.service.common.batch.domain.JobConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@SpringJUnitWebConfig
@RunWith(SpringRunner.class)
@WebMvcTest(JobExecutionController.class)
public class JobExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobExplorer jobExplorer;

    @MockBean
    private JobRegistry jobRegistry;

    @MockBean
    private JobStarter adHocStarter;

    @SpyBean
    private JobExecutionService jobExecutionService;

    @TestConfiguration
    static class AdditionalConfig {

        @Bean
        public JobExecutionService jobExecutionService(JobExplorer jobExplorer,
                                                       JobStarter jobStarter) {
            return new JobExecutionService(jobExplorer,
                                           new DefaultJobExecutionProvider(jobExplorer),
                                           jobStarter);
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        configureMock(jobExplorer);
        configureForJobExecutionsService(jobExplorer);
        configureMock(jobRegistry);
    }

    @Test
    public void jobExecutionById() throws Exception {
        when(jobExplorer.getJobExecution(je11.getId())).thenReturn(je11);
        mockMvc.perform(get("/jobExecutions/" + je11.getId()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$..jobExecution", hasSize(1)));
    }

    @Test
    public void jobExecutionByIdNotFound() throws Exception {
        mockMvc.perform(get("/jobExecutions/" + 10))
               .andExpect(status().isNotFound())
               .andExpect(content().string("{\"status\":\"404 NOT_FOUND\",\"message\":\"Could not find job execution with ID 10\",\"exception\":\"NoSuchJobExecutionException\",\"detail\":\"\"}"));
    }

    @Test
    public void jobExecutions() throws Exception {
        mockMvc.perform(get("/jobExecutions"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$..jobExecution", hasSize(5)));
    }

    @Test
    public void successfulJobExecutions() throws Exception {
        mockMvc.perform(get("/jobExecutions?exitCode=COMPLETED"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$..jobExecution", hasSize(3)));
    }

    @Test
    public void failedJobExecutions() throws Exception {
        mockMvc.perform(get("/jobExecutions?exitCode=FAILED"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$..jobExecution", hasSize(3)));
    }

    @Test
    public void successfulJobExecutionsPerJob() throws Exception {
        mockMvc.perform(get("/jobExecutions?jobName=j2&exitCode=COMPLETED"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$..jobExecution", hasSize(2)));
    }

    @Test
    public void successfulJobExecutionsPerJobAndLimited() throws Exception {
        mockMvc.perform(get("/jobExecutions?jobName=j2&exitCode=COMPLETED&limitPerJob=1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$..jobExecution", hasSize(1)));
    }

    @Test
    public void jobFailsWithDuplicateJobException() throws Exception {
        assertJobExecutionExceptionToStatusMapping(new DuplicateJobException("causeMsg"), HttpStatus.CONFLICT);
    }

    @Test
    public void jobFailsWithJobInstanceAlreadyCompleteException() throws Exception {
        assertJobExecutionExceptionToStatusMapping(new JobInstanceAlreadyCompleteException("causeMsg"),
                                                   HttpStatus.CONFLICT);
    }

    @Test
    public void jobFailsWithJobExecutionAlreadyRunningException() throws Exception {
        assertJobExecutionExceptionToStatusMapping(new JobExecutionAlreadyRunningException("causeMsg"),
                                                   HttpStatus.CONFLICT);
    }

    @Test
    public void jobFailsWithNoSuchJobException() throws Exception {
        assertJobExecutionExceptionToStatusMapping(new NoSuchJobException("causeMsg"), HttpStatus.NOT_FOUND);
    }

    @Test
    public void jobFailsWithJobParametersInvalidException() throws Exception {
        assertJobExecutionExceptionToStatusMapping(new JobParametersInvalidException("causeMsg"),
                                                   HttpStatus.BAD_REQUEST);
    }

    @Test
    public void jobFailsWithGenericException() throws Exception {
        when(adHocStarter.start(any(JobConfig.class))).thenThrow(new RuntimeException("msg",
                                                                                      new RuntimeException("cause")));
        mockMvc.perform(post("/jobExecutions").contentType(APPLICATION_JSON).content("{\"name\":\"foo\"}"))
               .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()))
               .andExpect(content().string("{\"status\":\"500 INTERNAL_SERVER_ERROR\",\"message\":\"msg\",\"exception\":\"RuntimeException\",\"detail\":\"cause\"}"));
    }

    private void assertJobExecutionExceptionToStatusMapping(JobExecutionException cause, HttpStatus expectedStatus)
                                                                                                                    throws Exception {
        when(adHocStarter.start(any(JobConfig.class))).thenThrow(new BatchRuntimeException("msg", cause));
        mockMvc.perform(post("/jobExecutions").contentType(APPLICATION_JSON).content("{\"name\":\"foo\"}"))
               .andExpect(status().is(expectedStatus.value()))
               .andExpect(content().string(String.format("{\"status\":\"%s\",\"message\":\"%s\",\"exception\":\"%s\",\"detail\":\"%s\"}",
                                                         expectedStatus.toString(),
                                                         cause.getMessage(),
                                                         cause.getClass().getSimpleName(),
                                                         "msg")));
    }
}