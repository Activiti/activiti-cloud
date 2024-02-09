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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.process.model.CloudProcessInstance;

public class CloudProcessInstanceImpl extends CloudRuntimeEntityImpl implements CloudProcessInstance {

    private String id;
    private String name;
    private Date startDate;
    private String initiator;

    @Schema(
        description = "The business key associated to the process instance. It could be useful to add a reference to external systems.",
        readOnly = true
    )
    private String businessKey;

    private ProcessInstanceStatus status;
    private String processDefinitionId;

    @Schema(
        description = "It identifies uniquely the process. In the BPMN process definition file it is the id attribute of a process and in the Modeling application it is usually called as Process ID."
    )
    private String processDefinitionKey;

    private String parentId;
    private Integer processDefinitionVersion;
    private String processDefinitionName;
    private Date completedDate;
    private Date suspendedDate;

    public CloudProcessInstanceImpl() {}

    public CloudProcessInstanceImpl(ProcessInstance processInstance) {
        super(processInstance);
        id = processInstance.getId();
        name = processInstance.getName();
        startDate = processInstance.getStartDate();
        initiator = processInstance.getInitiator();
        businessKey = processInstance.getBusinessKey();
        status = processInstance.getStatus();
        processDefinitionId = processInstance.getProcessDefinitionId();
        processDefinitionKey = processInstance.getProcessDefinitionKey();
        parentId = processInstance.getParentId();
        processDefinitionVersion = processInstance.getProcessDefinitionVersion();
        processDefinitionName = processInstance.getProcessDefinitionName();
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public Date getSuspendedDate() {
        return suspendedDate;
    }

    public void setSuspendedDate(Date suspendedDate) {
        this.suspendedDate = suspendedDate;
    }

    @Override
    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    @Override
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public ProcessInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessInstanceStatus status) {
        this.status = status;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    @Override
    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }
}
