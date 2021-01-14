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

package org.activiti.cloud.service.common.batch.job;

import static org.activiti.cloud.service.common.batch.Fixtures.configureMock;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.cloud.service.common.batch.controllers.JobController;
import org.activiti.cloud.service.common.batch.core.JobStarter;
import org.activiti.cloud.service.common.batch.core.job.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobController.class)
public class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobRegistry jobRegistry;

    @MockBean
    private JobStarter adHocStarter;

    @SpyBean
    private JobService jobService;

    @TestConfiguration
    static class AdditionalConfig {

        @Bean
        public JobService jobService(JobRegistry jobRegistry) {
            return new JobService(jobRegistry);
        }
    }

    @BeforeEach
    public void setUp() {
        configureMock(jobRegistry);
    }

    @Test
    public void jobs() throws Exception {
        mockMvc.perform(get("/v1/admin/batch/jobs"))
               .andDo(print())
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.*", hasSize(2)));

        verify(jobService).jobs();
    }

}
