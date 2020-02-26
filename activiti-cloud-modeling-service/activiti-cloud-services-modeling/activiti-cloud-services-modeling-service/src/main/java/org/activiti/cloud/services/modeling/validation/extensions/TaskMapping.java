/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.modeling.validation.extensions;

import java.util.Map;

import org.activiti.bpmn.model.FlowNode;
import org.activiti.cloud.modeling.api.process.ProcessVariableMapping;
import org.activiti.cloud.modeling.api.process.ServiceTaskActionType;

/**
 * Object that encapsulate task mappings information
 */
public class TaskMapping {

    private String processId;

    private FlowNode task;

    private ServiceTaskActionType action;

    private Map<String, ProcessVariableMapping> processVariableMappings;

    public TaskMapping(String processId,
                       FlowNode task,
                       ServiceTaskActionType action,
                       Map<String, ProcessVariableMapping> processVariableMappings) {
        this.processId = processId;
        this.task = task;
        this.action = action;
        this.processVariableMappings = processVariableMappings;
    }

    public String getProcessId() {
        return processId;
    }

    public FlowNode getTask() {
        return task;
    }

    public ServiceTaskActionType getAction() {
        return action;
    }

    public Map<String, ProcessVariableMapping> getProcessVariableMappings() {
        return processVariableMappings;
    }
}
