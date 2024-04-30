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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.Objects;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

//@FilterDef(
//    name = "variablesFilter",
//    parameters = { @ParamDef(name = "variableKeys", type = String.class) },
//    defaultCondition = "concat(process_definition_key, '/', name) in (:variableKeys)"
//)
@Entity(name = "ProcessVariable")
@Table(
    name = "PROCESS_VARIABLE",
    indexes = {
        @Index(name = "proc_var_processInstanceId_idx", columnList = "processInstanceId", unique = false),
        @Index(name = "proc_var_name_idx", columnList = "name", unique = false),
        @Index(name = "proc_var_executionId_idx", columnList = "executionId", unique = false),
    }
)
@DynamicInsert
@DynamicUpdate
public class ProcessVariableEntity extends AbstractVariableEntity {

    @Id
    @GeneratedValue(generator = "process_variable_sequence", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
        name = "process_variable_sequence",
        sequenceName = "process_variable_sequence",
        allocationSize = 50
    )
    private Long id;

    private String variableDefinitionId;

    @Schema(
        description = "The business key associated to the process instance. It could be useful to add a reference to external systems.",
        readOnly = true
    )
    private String processDefinitionKey;

    public ProcessVariableEntity() {}

    public ProcessVariableEntity(
        Long id,
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
        super(
            type,
            name,
            processInstanceId,
            serviceName,
            serviceFullName,
            serviceVersion,
            appName,
            appVersion,
            createTime,
            lastUpdatedTime,
            executionId
        );
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getTaskId() {
        return null;
    }

    @Override
    public boolean isTaskVariable() {
        return false;
    }

    public String getVariableDefinitionId() {
        return variableDefinitionId;
    }

    public void setVariableDefinitionId(String variableDefinitionId) {
        this.variableDefinitionId = variableDefinitionId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
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
        ProcessVariableEntity other = (ProcessVariableEntity) obj;

        return this.getId() != null && Objects.equals(this.getId(), other.getId());
    }
}
