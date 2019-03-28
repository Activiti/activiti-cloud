/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.organization.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.process.Extensions;
import org.activiti.cloud.services.organization.jpa.audit.AuditableEntity;
import org.activiti.cloud.services.organization.jpa.version.VersionedEntity;
import org.hibernate.annotations.GenericGenerator;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Model model entity
 */
@Entity(name = "Model")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ModelEntity extends AuditableEntity<String> implements Model<ProjectEntity, String>,
                                                                    VersionedEntity<ModelVersionEntity> {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @JsonIgnore
    private ProjectEntity project;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL)
    private List<ModelVersionEntity> versions = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JsonIgnore
    private ModelVersionEntity latestVersion = new ModelVersionEntity();

    private String type;

    private String name;

    private String template;

    public ModelEntity() { // for JPA
    }

    public ModelEntity(String name,
                       String type) {
        this.name = name;
        this.type = type;
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public ProjectEntity getProject() {
        return project;
    }

    @Override
    public void setProject(ProjectEntity project) {
        this.project = project;
    }

    @Transient
    @JsonProperty("projectId")
    public String projectId() {
        return Optional.ofNullable(project)
                .map(ProjectEntity::getId)
                .orElse(null);
    }

    @Transient
    @Override
    public String getVersion() {
        return latestVersion.getVersion();
    }

    @Override
    @JsonIgnore
    public String getContentType() {
        return latestVersion.getContentType();
    }

    @Override
    public void setContentType(String contentType) {
        latestVersion.setContentType(contentType);
    }

    @Override
    @JsonIgnore
    public String getContent() {
        return latestVersion.getContent();
    }

    @Override
    public void setContent(String content) {
        latestVersion.setContent(content);
    }

    @Override
    public Extensions getExtensions() {
        return latestVersion.getExtensions();
    }

    @Override
    public void setExtensions(Extensions extensions) {
        latestVersion.setExtensions(extensions);
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(String template) {
        this.template = template;
    }

    @Override
    public List<ModelVersionEntity> getVersions() {
        return versions;
    }

    public void setVersions(List<ModelVersionEntity> versions) {
        this.versions = versions;
    }

    @Override
    public void setLatestVersion(ModelVersionEntity latestVersion) {
        this.latestVersion = latestVersion;
    }

    @Override
    public ModelVersionEntity getLatestVersion() {
        return latestVersion;
    }
}
