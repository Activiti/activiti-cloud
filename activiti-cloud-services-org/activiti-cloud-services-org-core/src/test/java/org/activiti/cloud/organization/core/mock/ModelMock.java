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

package org.activiti.cloud.organization.core.mock;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.core.rest.resource.EntityWithRestResource;
import org.activiti.cloud.organization.core.rest.resource.RestResource;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 *
 */
@EntityWithRestResource
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ModelMock extends AuditableMock implements Model<ApplicationMock, String> {

    private String id;

    private ApplicationMock application;

    private ModelType type;

    @Transient
    @JsonIgnore
    @RestResource(
            resourceIdField = "id",
            resourceKeyField = "type")
    private ModelReference data;

    public ModelMock() { // for JPA
        this.data = new ModelReference();
    }

    public ModelMock(String id,
                     String name,
                     ModelType type) {
        this.id = id;
        this.type = type;
        this.data = new ModelReference(id,
                                       name);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
        data.setModelId(id);
    }

    @Override
    public String getName() {
        return data.getName();
    }

    @Override
    public void setName(String name) {
        data.setName(name);
    }

    @Override
    public ModelType getType() {
        return type;
    }

    @Override
    public void setType(ModelType type) {
        this.type = type;
    }

    public ModelReference getData() {
        return data;
    }

    public void setData(ModelReference data) {
        this.data = data;
    }

    @Override
    public ApplicationMock getApplication() {
        return application;
    }

    @Override
    public void setApplication(ApplicationMock application) {
        this.application = application;
    }

    @Override
    public String getVersion() {
        return data.getVersion();
    }

    @Override
    public void setVersion(String version) {
        data.setVersion(version);
    }

    @Override
    public String getContentType() {
        return data.getContentType();
    }

    @Override
    public void setContentType(String contentType) {
        data.setContentType(contentType);
    }

    @Override
    public String getContent() {
        return data.getContent();
    }

    @Override
    public void setContent(String content) {
        data.setContent(content);
    }
}
