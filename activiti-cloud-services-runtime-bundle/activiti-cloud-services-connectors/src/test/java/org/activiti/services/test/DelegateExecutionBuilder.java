/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.services.test;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.delegate.DelegateExecution;

import static org.mockito.Mockito.*;

public class DelegateExecutionBuilder {

        private DelegateExecution execution;

        private DelegateExecutionBuilder() {
            execution = mock(DelegateExecution.class);
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

        public DelegateExecutionBuilder withFlowNodeId(String flowNodeId) {
            when(execution.getCurrentActivityId()).thenReturn(flowNodeId);
            return this;
        }

        public DelegateExecutionBuilder withServiceTask(ServiceTask serviceTask) {
            when(execution.getCurrentFlowElement()).thenReturn(serviceTask);
            return this;
        }
        
        public DelegateExecution  build() {
            return execution;
        }
    }
