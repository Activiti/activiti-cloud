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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.services.modeling.jpa.audit.AuditableEntity;
import org.activiti.cloud.services.modeling.jpa.version.VersionedEntity;
import org.hibernate.annotations.GenericGenerator;

/**
 * Model model entity
 */
@Entity(name = "Model")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Table(name = "Model")
public class ModelEntity extends AuditableEntity<String> implements Model<ProjectEntity, String>,
    VersionedEntity<ModelVersionEntity> {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    private String id;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "project_models",
        joinColumns = {@JoinColumn(name = "models_id")},
        inverseJoinColumns = {@JoinColumn(name = "project_id")}
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

    @Enumerated(EnumType.ORDINAL)
    private ModelScope scope = ModelScope.PROJECT;

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
    public Set<ProjectEntity> getProjects() {
        return projects;
    }

    public void setProjects(Set<ProjectEntity> projects) {
        this.projects = projects;
    }

    @Override
    public void addProject(ProjectEntity project) {
        if(project!=null) {
            if(projects == null){
                projects = new HashSet<>();
            }
            if(!projects.contains(project)){
                projects.add(project);
                project.addModel(this);
            }
        }
    }

    @Override
    public void removeProject(ProjectEntity project) {
        if(projects!=null && projects.contains(project)){
            projects.remove(project);
            project.removeModel(this);
        }
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
