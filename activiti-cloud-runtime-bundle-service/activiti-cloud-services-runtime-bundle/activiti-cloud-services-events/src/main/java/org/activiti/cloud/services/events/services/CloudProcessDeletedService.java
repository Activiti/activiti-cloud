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

import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.ManagementService;

public class CloudProcessDeletedService {

    private final ManagementService managementService;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;

    public CloudProcessDeletedService(
        ManagementService managementService,
        ProcessEngineEventsAggregator processEngineEventsAggregator
    ) {
        this.managementService = managementService;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
    }

    public void delete(String processInstanceId) {
        managementService.executeCommand(
            new DeleteCloudProcessInstanceCmd(processInstanceId, processEngineEventsAggregator)
        );
    }

    public void sendDeleteEvent(String processInstanceId) {
        managementService.executeCommand(
            new SendDeleteCloudProcessInstanceEventCmd(processInstanceId, processEngineEventsAggregator)
        );
    }
}
