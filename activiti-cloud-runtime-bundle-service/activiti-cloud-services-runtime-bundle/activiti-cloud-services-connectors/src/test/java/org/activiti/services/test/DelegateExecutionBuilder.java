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
package org.activiti.services.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

public class DelegateExecutionBuilder {

    private ExecutionEntity execution;
    private ExecutionEntity processInstance;

    private DelegateExecutionBuilder() {
        execution = mock(ExecutionEntity.class);
        processInstance = mock(ExecutionEntity.class);

        when(execution.getProcessInstance()).thenReturn(processInstance);
    }

    public static DelegateExecutionBuilder anExecution() {
        return new DelegateExecutionBuilder();
    }

    public DelegateExecutionBuilder withId(String id) {
        when(execution.getId()).thenReturn(id);
        return this;
    }

    public DelegateExecutionBuilder withProcessDefinitionId(String processDefinitionId) {
        when(execution.getProcessDefinitionId()).thenReturn(processDefinitionId);
        return this;
    }

    public DelegateExecutionBuilder withProcessInstanceId(String processInstanceId) {
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        return this;
    }

    public DelegateExecutionBuilder withRootProcessInstanceId(String rootProcessInstanceId) {
        when(execution.getRootProcessInstanceId()).thenReturn(rootProcessInstanceId);
        return this;
    }

    public DelegateExecutionBuilder withBusinessKey(String businessKey) {
        when(execution.getProcessInstanceBusinessKey()).thenReturn(businessKey);
        return this;
    }

    public DelegateExecutionBuilder withFlowNodeId(String flowNodeId) {
        when(execution.getCurrentActivityId()).thenReturn(flowNodeId);
        return this;
    }

    public DelegateExecutionBuilder withServiceTask(ServiceTask serviceTask) {
        when(execution.getCurrentFlowElement()).thenReturn(serviceTask);
        return this;
    }

    public DelegateExecution build() {
        return execution;
    }

    public DelegateExecutionBuilder withProcessDefinitionKey(String processDefinitionKey) {
        when(processInstance.getProcessDefinitionKey()).thenReturn(processDefinitionKey);
        return this;
    }

    public DelegateExecutionBuilder withParentProcessInstanceId(String parentProcessInstanceId) {
        when(processInstance.getParentProcessInstanceId()).thenReturn(parentProcessInstanceId);
        return this;
    }
}
