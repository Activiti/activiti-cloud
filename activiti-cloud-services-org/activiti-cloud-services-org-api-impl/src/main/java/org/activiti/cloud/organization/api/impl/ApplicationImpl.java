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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.services.auditable.AbstractAuditable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Implementation for {@link Application}
 */
@ApiModel("Application")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ApplicationImpl extends AbstractAuditable<String> implements Application<String> {

    @ApiModelProperty(value = "The unique identifier of the application", readOnly = true)
    private String id;

    @ApiModelProperty(value = "The name of the application")
    private String name;

    public ApplicationImpl() {

    }

    public ApplicationImpl(String id,
                           String name) {
        this.id = id;
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
