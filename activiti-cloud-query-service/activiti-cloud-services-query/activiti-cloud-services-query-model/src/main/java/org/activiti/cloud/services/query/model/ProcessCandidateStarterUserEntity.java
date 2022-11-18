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

import com.fasterxml.jackson.annotation.*;
import java.util.Objects;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity(name = "ProcessCandidateStarterUser")
@IdClass(ProcessCandidateStarterUserId.class)
@Table(
    name = "PROCESS_CANDIDATE_STARTER_USER",
    indexes = {
        @Index(name = "pcsu_userId_idx", columnList = "userId", unique = false),
        @Index(name = "pcsu_processDefinition_idx", columnList = "processDefinitionId", unique = false)
    }
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@DynamicInsert
@DynamicUpdate
public class ProcessCandidateStarterUserEntity {

    @Id
    private String processDefinitionId;

    @Id
    private String userId;

    @JsonIgnore
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processDefinitionId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        nullable = true,
        foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private ProcessDefinitionEntity processDefinition;

    @JsonCreator
    public ProcessCandidateStarterUserEntity(
        @JsonProperty("processDefinitionId") String processDefinitionId,
        @JsonProperty("userId") String userId
    ) {
        this.processDefinitionId = processDefinitionId;
        this.userId = userId;
    }

    public ProcessCandidateStarterUserEntity() {}

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ProcessDefinitionEntity getProcessDefinition() {
        return this.processDefinition;
    }

    public void setProcessDefinition(ProcessDefinitionEntity processDefinition) {
        this.processDefinition = processDefinition;
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
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ProcessCandidateStarterUserEntity other = (ProcessCandidateStarterUserEntity) obj;
        return Objects.equals(processDefinitionId, other.processDefinitionId) && Objects.equals(userId, other.userId);
    }
}
