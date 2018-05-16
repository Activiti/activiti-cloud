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

package org.activiti.cloud.organization.core.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.activiti.cloud.organization.core.rest.resource.EntityWithRestResource;
import org.activiti.cloud.organization.core.rest.resource.RestResource;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Model model entity
 */
@Entity
@EntityWithRestResource
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class Model {

    @Id
    private String id;

    @ManyToOne
    private Project project;

    private ModelType type;

    private String refId;

    @Transient
    @JsonUnwrapped
    @RestResource(
            resourceIdField = "refId",
            resourceKeyField = "type")
    private ModelReference data;

    public Model() { // for JPA
    }

    public Model(String id,
                 String name,
                 ModelType type,
                 String refId) {
        this.id = id;
        this.type = type;
        this.refId = refId;
        this.data = new ModelReference(refId,
                                       name);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ModelType getType() {
        return type;
    }

    public void setType(ModelType type) {
        this.type = type;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public ModelReference getData() {
        return data;
    }

    public void setData(ModelReference data) {
        this.data = data;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public enum ModelType {
        FORM,
        PROCESS_MODEL
    }
}
