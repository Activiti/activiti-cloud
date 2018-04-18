/*
 * Copyright 2018 Alfresco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.events;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;

public abstract class AbstractProcessEngineEvent implements ProcessEngineEvent {

    private Service service;
    private Application application;
    private String executionId;
    private String processDefinitionId;
    private String processInstanceId;
    private Long timestamp;

    public AbstractProcessEngineEvent() {
    }

    public AbstractProcessEngineEvent(Service service,
                                      Application application,
                                      String executionId,
                                      String processDefinitionId,
                                      String processInstanceId) {
        this.service = service;
        this.application = application;
        this.executionId = executionId;
        this.processDefinitionId = processDefinitionId;
        this.processInstanceId = processInstanceId;
        this.timestamp = System.currentTimeMillis();
    }

    public AbstractProcessEngineEvent(Service service,
                                      Application application,
                                      String executionId,
                                      String processDefinitionId,
                                      String processInstanceId,
                                      Long timestamp) {
        this.service = service;
        this.application = application;
        this.executionId = executionId;
        this.processDefinitionId = processDefinitionId;
        this.processInstanceId = processInstanceId;
        this.timestamp = timestamp;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public Application getApplication() {
        return application;
    }
}
