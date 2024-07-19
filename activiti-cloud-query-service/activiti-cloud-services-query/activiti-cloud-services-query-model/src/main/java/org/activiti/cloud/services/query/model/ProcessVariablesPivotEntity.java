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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "ProcessVariablesPivot")
@Table(
    name = "process_variable_pivot",
    indexes = { @Index(name = "proc_var_processInstanceId_idx", columnList = "processInstanceId", unique = true) }
)
public class ProcessVariablesPivotEntity {

    @Id
    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "process_definition_key")
    private String processDefinitionKey;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private ProcessInstanceEntity processInstance;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "`process_variables`", columnDefinition = "jsonb")
    private Map<String, ProcessVariableInstance> values;

    @OneToMany
    @JoinColumn(name = "process_instance_id", referencedColumnName = "process_instance_id")
    private Set<TaskEntity> tasks;

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public Map<String, ProcessVariableInstance> getValues() {
        return values;
    }

    public void setValues(Map<String, ProcessVariableInstance> values) {
        this.values = values;
    }

    public Set<TaskEntity> getTasks() {
        return tasks;
    }

    public void setTasks(Set<TaskEntity> tasks) {
        this.tasks = tasks;
    }

    public void setProcessInstance(ProcessInstanceEntity processInstance) {
        this.processInstance = processInstance;
    }

    public ProcessInstanceEntity getProcessInstance() {
        return processInstance;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }
}
