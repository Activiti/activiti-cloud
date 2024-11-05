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
package org.activiti.cloud.services.events.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.ManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CloudProcessDeletedServiceTest {

    private CloudProcessDeletedService cloudProcessDeletedService;

    @Mock
    private ManagementService managementService;

    @Mock
    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @BeforeEach
    public void setUp() {
        cloudProcessDeletedService = new CloudProcessDeletedService(managementService, processEngineEventsAggregator);
    }

    @Test
    public void should_sendDeleteEvent() {
        //given

        //when
        cloudProcessDeletedService.sendDeleteEvent("1");

        //then
        verify(managementService).executeCommand(any(SendDeleteCloudProcessInstanceEventCmd.class));
    }

    @Test
    public void should_deleteProcessInstance() {
        //given

        //when
        cloudProcessDeletedService.delete("1");

        //then
        verify(managementService).executeCommand(any(DeleteCloudProcessInstanceCmd.class));
    }
}
