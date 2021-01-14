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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.activiti.cloud.service.common.batch.core.job.JobService;
import org.activiti.cloud.service.common.batch.domain.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.configuration.JobRegistry;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {

    @Mock
    private JobRegistry jobRegistry;

    private JobService jobService;

    @BeforeEach
    public void setUp() {
        configureMock(jobRegistry);
        jobService = new JobService(jobRegistry);
    }

    @Test
    public void jobs() {
        Collection<Job> jobs = jobService.jobs();
        assertThat(jobs).hasSize(2);

    }
}