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

package org.activiti.cloud.organization.api.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.services.auditable.AbstractAuditable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Implementation for {@link Model}
 */
@ApiModel("Model")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ModelImpl extends AbstractAuditable<String> implements Model<ApplicationImpl, String> {

    @ApiModelProperty(readOnly = true)
    private String id;

    @ApiModelProperty("The name of the model")
    private String name;

    @ApiModelProperty(value = "The type of the model", readOnly = true)
    private ModelType type;

    @ApiModelProperty(value = "The version of the model", readOnly = true)
    private String version;

    @ApiModelProperty(value = "The content type of the model", readOnly = true, hidden = true)
    @JsonIgnore
    private String contentType;

    @ApiModelProperty(value = "The content of the model", readOnly = true, hidden = true)
    @JsonIgnore
    private String content;

    @ApiModelProperty(hidden = true)
    private ApplicationImpl application;

    public ModelImpl() {

    }

    public ModelImpl(String id,
                     String name,
                     ModelType type) {
        this.id = id;
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
    public ModelType getType() {
        return type;
    }

    @Override
    public void setType(ModelType type) {
        this.type = type;
    }

    @Override
    public ApplicationImpl getApplication() {
        return application;
    }

    @Override
    public void setApplication(ApplicationImpl application) {
        this.application = application;
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
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return type + " MODEL [" + id + ", " + name + "]";
    }
}
