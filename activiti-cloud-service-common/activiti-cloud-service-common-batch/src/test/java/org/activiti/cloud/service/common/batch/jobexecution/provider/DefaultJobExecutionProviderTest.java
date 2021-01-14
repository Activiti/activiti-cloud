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

import org.activiti.cloud.service.common.batch.Fixtures;
import org.activiti.cloud.service.common.batch.core.jobexecution.provider.DefaultJobExecutionProvider;
import org.activiti.cloud.service.common.batch.core.jobexecution.provider.JobExecutionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.explore.JobExplorer;

@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultJobExecutionProviderTest extends AbstractJobExecutionProviderTest {

    @Mock
    private JobExplorer jobExplorer;

    private DefaultJobExecutionProvider provider;

    @BeforeEach
    public void setUp() {
        Fixtures.configureMock(jobExplorer);
        Fixtures.configureForJobExecutionsService(jobExplorer);
        provider = new DefaultJobExecutionProvider(jobExplorer);
    }

    @Override
    protected JobExecutionProvider provider() {
        return provider;
    }
}