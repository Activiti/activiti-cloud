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
package org.activiti.cloud.services.query.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;
import java.util.Objects;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.format.annotation.DateTimeFormat;

@MappedSuperclass
public abstract class AbstractVariableEntity extends ActivitiEntityMetadata implements CloudVariableInstance {

    private String type;

    private String name;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date createTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date lastUpdatedTime;

    private String executionId;

    @Convert(converter = VariableValueJsonConverter.class)
    @Column(columnDefinition = "text")
    private VariableValue<?> value;

    private Boolean markedAsDeleted = false;

    private String processInstanceId;

    @JsonIgnore
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processInstanceId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private ProcessInstanceEntity processInstance;

    public AbstractVariableEntity() {}

    public AbstractVariableEntity(
        String type,
        String name,
        String processInstanceId,
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion,
        Date createTime,
        Date lastUpdatedTime,
        String executionId
    ) {
        super(serviceName, serviceFullName, serviceVersion, appName, appVersion);
        this.type = type;
        this.name = name;
        this.processInstanceId = processInstanceId;
        this.createTime = createTime;
        this.lastUpdatedTime = lastUpdatedTime;
        this.executionId = executionId;
    }

    public abstract Long getId();

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(Date lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public <T> void setValue(T value) {
        this.value = new VariableValue<>(value);
    }

    @Override
    public <T> T getValue() {
        return (T) value.getValue();
    }

    public Boolean getMarkedAsDeleted() {
        return markedAsDeleted;
    }

    public void setMarkedAsDeleted(Boolean markedAsDeleted) {
        this.markedAsDeleted = markedAsDeleted;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public ProcessInstanceEntity getProcessInstance() {
        return processInstance;
    }

    public void setProcessInstance(ProcessInstanceEntity processInstance) {
        this.processInstance = processInstance;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractVariableEntity other = (AbstractVariableEntity) obj;
        return getId() != null && Objects.equals(getId(), other.getId());
    }
}
