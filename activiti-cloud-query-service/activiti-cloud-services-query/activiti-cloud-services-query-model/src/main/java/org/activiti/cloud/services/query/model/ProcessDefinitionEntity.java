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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity(name = "ProcessDefinition")
@Table(
    name = "PROCESS_DEFINITION",
    indexes = {
        @Index(name = "pd_name_idx", columnList = "name"),
        @Index(name = "pd_key_idx", columnList = "processDefinitionKey")
    }
)
@DynamicInsert
@DynamicUpdate
public class ProcessDefinitionEntity extends ActivitiEntityMetadata implements CloudProcessDefinition {

    @Id
    private String id;

    private String name;

    @Column(name = "processDefinitionKey")
    private String key;

    private String description;
    private int version;
    private String formKey;
    private String category;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processDefinitionId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<ProcessCandidateStarterUserEntity> candidateStarterUsers = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processDefinitionId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<ProcessCandidateStarterGroupEntity> candidateStarterGroups = new LinkedHashSet<>();

    public ProcessDefinitionEntity() {}

    public ProcessDefinitionEntity(
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion
    ) {
        super(serviceName, serviceFullName, serviceVersion, appName, appVersion);
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
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Set<ProcessCandidateStarterUserEntity> getCandidateStarterUsers() {
        return candidateStarterUsers;
    }

    public Set<ProcessCandidateStarterGroupEntity> getCandidateStarterGroups() {
        return candidateStarterGroups;
    }

    public void setCandidateStarterUsers(Set<ProcessCandidateStarterUserEntity> candidateStarterUsers) {
        this.candidateStarterUsers = candidateStarterUsers;
    }

    public void setCandidateStarterGroups(Set<ProcessCandidateStarterGroupEntity> candidateStarterGroups) {
        this.candidateStarterGroups = candidateStarterGroups;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        ProcessDefinitionEntity other = (ProcessDefinitionEntity) obj;
        return id != null && Objects.equals(id, other.id);
    }
}
