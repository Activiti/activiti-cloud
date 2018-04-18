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

import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.api.model.Service;

public class ProcessSuspendedEventImpl extends AbstractProcessEngineEvent implements ProcessSuspendedEvent {

    private ProcessInstance processInstance;

    public ProcessSuspendedEventImpl() {
    }

    public ProcessSuspendedEventImpl(Service service,
                                     Application application,
                                     String executionId,
                                     String processDefinitionId,
                                     String processInstanceId,
                                     ProcessInstance processInstance) {
        super(service,
              application,
              executionId,
              processDefinitionId,
              processInstanceId);
        this.processInstance = processInstance;
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    @Override
    public String getEventType() {
        return "ProcessSuspendedEvent";
    }

    @Override
    public String toString() {
        return "ProcessSuspendedEventImpl{" +
                    "processInstance=" + processInstance +
                    ", service=" + getService() + '\'' +
                    ", application=" + getApplication() + '\'' +
                    ", executionId='" + getExecutionId() + '\'' +
                    ", processDefinitionId='" + getProcessDefinitionId() + '\'' +
                    ", processInstanceId='" + getProcessInstanceId() + '\'' +
                    ", timestamp=" + getTimestamp() +
                '}';
    }
}
