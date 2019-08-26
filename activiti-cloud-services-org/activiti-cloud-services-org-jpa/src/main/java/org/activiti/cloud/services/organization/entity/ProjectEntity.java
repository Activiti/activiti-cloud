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

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.services.organization.jpa.audit.AuditableEntity;
import org.hibernate.annotations.GenericGenerator;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.activiti.cloud.organization.validation.ValidationUtil.DNS_LABEL_REGEX;
import static org.activiti.cloud.organization.validation.ValidationUtil.NAME_MAX_LENGTH;
import static org.activiti.cloud.organization.validation.ValidationUtil.PROJECT_INVALID_EMPTY_NAME;
import static org.activiti.cloud.organization.validation.ValidationUtil.PROJECT_INVALID_NAME_LENGTH_MESSAGE;
import static org.activiti.cloud.organization.validation.ValidationUtil.PROJECT_INVALID_NAME_MESSAGE;

/**
 * Project model entity
 */
@Entity(name = "Project")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ProjectEntity extends AuditableEntity<String> implements Project<String> {

    @OneToMany
    @JsonIgnore
    private List<ModelEntity> models;

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    private String id;

    @Column(unique = true)
    @Pattern(regexp = DNS_LABEL_REGEX, message = PROJECT_INVALID_NAME_MESSAGE)
    @Size(max = NAME_MAX_LENGTH, message = PROJECT_INVALID_NAME_LENGTH_MESSAGE)
    @NotEmpty(message = PROJECT_INVALID_EMPTY_NAME)
    private String name;

    private String description;

    private String version;

    public ProjectEntity() {  // for JPA
    }

    public ProjectEntity(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public List<ModelEntity> getModels() {
        return models;
    }

    public void setModels(List<ModelEntity> models) {
        this.models = models;
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
}
