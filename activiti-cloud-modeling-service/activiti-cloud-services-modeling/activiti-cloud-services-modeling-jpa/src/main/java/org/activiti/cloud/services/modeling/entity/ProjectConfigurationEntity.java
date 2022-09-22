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
package org.activiti.cloud.services.modeling.entity;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.ProjectConfiguration;

@Entity(name = "ProjectConfiguration")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Table(name = "PROJECT_CONFIGURATION")
public class ProjectConfigurationEntity implements ProjectConfiguration {

    @Id
    @JsonIgnore
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    @JsonIgnore
    private ProjectEntity project;

    Boolean enableCandidateStarters;

    public ProjectConfigurationEntity() {  // for JPA
    }

    public ProjectConfigurationEntity(Boolean enableCandidateStarters) {
        this.enableCandidateStarters = enableCandidateStarters;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setEnableCandidateStarters(Boolean enableCandidateStarters) {
        this.enableCandidateStarters = enableCandidateStarters;
    }

    @Override
    public Boolean getEnableCandidateStarters() {
        return enableCandidateStarters;
    }

    @Override
    public void setProject(Project project) {
        this.project = (ProjectEntity) project;
    }

    @Override
    public ProjectEntity getProject() {
        return project;
    }
}
