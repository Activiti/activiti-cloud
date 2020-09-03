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
package org.activiti.cloud.api.process.model.impl;

import java.util.Objects;

import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;

public class CloudBPMNActivityImpl extends CloudRuntimeEntityImpl implements CloudBPMNActivity {

    private String id;
    private String activityName;
    private String activityType;
    private String executionId;
    private String elementId;
    private String processDefinitionId;
    private String processInstanceId;
    private CloudBPMNActivity.BPMNActivityStatus status;

    public CloudBPMNActivityImpl() { }

    @Override
    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


    @Override
    public String getActivityName() {
        return activityName;
    }


    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }


    @Override
    public String getActivityType() {
        return activityType;
    }


    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }


    @Override
    public String getExecutionId() {
        return executionId;
    }


    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }


    @Override
    public String getElementId() {
        return elementId;
    }


    public void setElementId(String elementId) {
        this.elementId = elementId;
    }


    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }


    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }


    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }


    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public CloudBPMNActivity.BPMNActivityStatus getStatus() {
        return status;
    }


    public void setStatus(CloudBPMNActivity.BPMNActivityStatus status) {
        this.status = status;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(activityName,
                                               activityType,
                                               elementId,
                                               executionId,
                                               id,
                                               processDefinitionId,
                                               processInstanceId,
                                               status);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CloudBPMNActivityImpl other = (CloudBPMNActivityImpl) obj;
        return Objects.equals(activityName, other.activityName) &&
               Objects.equals(activityType, other.activityType) &&
               Objects.equals(elementId, other.elementId) &&
               Objects.equals(executionId, other.executionId) &&
               Objects.equals(id, other.id) &&
               Objects.equals(processDefinitionId, other.processDefinitionId) &&
               Objects.equals(processInstanceId, other.processInstanceId) &&
               Objects.equals(status, other.status);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CloudBPMNActivityImpl [id=")
               .append(id)
               .append(", activityName=")
               .append(activityName)
               .append(", activityType=")
               .append(activityType)
               .append(", executionId=")
               .append(executionId)
               .append(", elementId=")
               .append(elementId)
               .append(", processDefinitionId=")
               .append(processDefinitionId)
               .append(", processInstanceId=")
               .append(processInstanceId)
               .append(", status=")
               .append(status)
               .append(", super()=")
               .append(super.toString())
               .append("]");
        return builder.toString();
    }

}
