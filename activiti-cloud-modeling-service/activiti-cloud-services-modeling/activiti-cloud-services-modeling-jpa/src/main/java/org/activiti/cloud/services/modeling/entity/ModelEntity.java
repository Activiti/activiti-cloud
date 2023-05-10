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
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.services.modeling.jpa.audit.AuditableEntity;
import org.activiti.cloud.services.modeling.jpa.version.VersionedEntity;
import org.hibernate.annotations.UuidGenerator;

/**
 * Model model entity
 */
@Entity(name = "Model")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Table(name = "Model")
public class ModelEntity
    extends AuditableEntity<String>
    implements Model<ProjectEntity, String>, VersionedEntity<ModelVersionEntity> {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "project_models",
        joinColumns = { @JoinColumn(name = "models_id") },
        inverseJoinColumns = { @JoinColumn(name = "project_id") }
    )
    private Set<ProjectEntity> projects = new HashSet<>();

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL)
    private List<ModelVersionEntity> versions = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JsonIgnore
    private ModelVersionEntity latestVersion = new ModelVersionEntity();

    private String type;

    private String name;

    private String template;

    private String category;

    @Enumerated(EnumType.ORDINAL)
    private ModelScope scope = ModelScope.PROJECT;

    public ModelEntity() {} // for JPA

    public ModelEntity(String name, String type) {
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
    public Set<ProjectEntity> getProjects() {
        return projects;
    }

    public void setProjects(Set<ProjectEntity> projects) {
        this.projects = projects;
    }

    @Override
    public void addProject(ProjectEntity project) {
        if (project != null) {
            if (projects == null) {
                projects = new HashSet<>();
            }
            if (!projects.contains(project)) {
                projects.add(project);
                project.addModel(this);
            }
        }
    }

    @Override
    public void removeProject(ProjectEntity project) {
        if (projects != null && projects.contains(project)) {
            projects.remove(project);
            project.removeModel(this);
        }
    }

    @Override
    public void clearProjects() {
        projects.clear();
    }

    @Transient
    @JsonProperty("projectIds")
    public Set<String> getProjectIds() {
        if (projects != null && !projects.isEmpty()) {
            Set<String> projectIds = new HashSet<>();
            projects.forEach(project -> projectIds.add(project.getId()));
            return projectIds;
        } else {
            return null;
        }
    }

    public void setProjectIds(Set<String> projectsId) {
        // Defined only for avoiding Jackson deserialization issues
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
    public byte[] getContent() {
        return latestVersion.getContent();
    }

    @Override
    public void setContent(byte[] content) {
        latestVersion.setContent(content);
    }

    @Override
    public Map<String, Object> getExtensions() {
        return latestVersion.getExtensions();
    }

    @Override
    public void setExtensions(Map<String, Object> extensions) {
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
    public ModelScope getScope() {
        return scope;
    }

    @Override
    public void setScope(ModelScope scope) {
        this.scope = scope;
    }

    @Override
    public String getCategory() {
        return this.category;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public boolean hasProjects() {
        return getProjects() != null && !getProjects().isEmpty();
    }

    @Override
    public boolean hasMultipleProjects() {
        return getProjects() != null && getProjects().size() > 1;
    }

    @Override
    public List<ModelVersionEntity> getVersions() {
        return versions;
    }

    @Override
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
