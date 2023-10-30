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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;
import org.activiti.cloud.modeling.api.ModelValidationErrorProducer;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.modeling.jpa.audit.AuditableEntity;
import org.hibernate.annotations.GenericGenerator;

/**
 * Project model entity
 */
@Table(name = "Project", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "createdBy" }))
@Entity(name = "Project")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ProjectEntity extends AuditableEntity<String> implements Project<String>, ModelValidationErrorProducer {

    @JsonIgnore
    @ManyToMany(mappedBy = "projects")
    private Set<ModelEntity> models = new HashSet<>();

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    private String id;

    @Transient
    private String name;

    @Column(name = "disp_name")
    private String displayName;

    @Column(name = "name")
    private String key;

    private String description;

    private String version;

    public ProjectEntity() {} // for JPA

    public ProjectEntity(String name, String key) {
        this.displayName = name;
        this.key = key;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Set<ModelEntity> getModels() {
        return models;
    }

    public void setModels(Set<ModelEntity> models) {
        this.models = models;
    }

    @Transient
    @Override
    public String getName() {
        return displayName;
    }

    @Override
    public void setName(String name) {
        this.displayName = name;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public void addModel(ModelEntity model) {
        if (!models.contains(model)) {
            models.add(model);
            model.addProject(this);
        }
    }

    public void removeModel(ModelEntity model) {
        if (models.contains(model)) {
            models.remove(model);
            model.removeProject(this);
        }
    }
}
