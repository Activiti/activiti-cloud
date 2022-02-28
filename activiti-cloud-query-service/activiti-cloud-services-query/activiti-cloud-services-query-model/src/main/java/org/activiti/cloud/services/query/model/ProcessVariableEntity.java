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

import java.util.Date;
import java.util.Objects;
import javax.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity(name = "ProcessVariable")
@Table(
    name = "PROCESS_VARIABLE",
    indexes = {
        @Index(
            name = "proc_var_processInstanceId_idx",
            columnList = "processInstanceId",
            unique = false
        ),
        @Index(name = "proc_var_name_idx", columnList = "name", unique = false),
        @Index(
            name = "proc_var_executionId_idx",
            columnList = "executionId",
            unique = false
        ),
    }
)
@DynamicInsert
@DynamicUpdate
public class ProcessVariableEntity extends AbstractVariableEntity {

    @Id
    @GeneratedValue(
        generator = "process_variable_sequence",
        strategy = GenerationType.SEQUENCE
    )
    @SequenceGenerator(
        name = "process_variable_sequence",
        sequenceName = "process_variable_sequence",
        allocationSize = 50
    )
    private Long id;

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

        return (
            this.getId() != null && Objects.equals(this.getId(), other.getId())
        );
    }
}
