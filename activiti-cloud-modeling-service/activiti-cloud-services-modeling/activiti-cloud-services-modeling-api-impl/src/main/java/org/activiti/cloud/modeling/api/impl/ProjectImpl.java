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
package org.activiti.cloud.modeling.api.impl;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.auditable.AbstractAuditable;

/**
 * Implementation for {@link Project}
 */
@Schema(name = "Project")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ProjectImpl extends AbstractAuditable<String> implements Project<String> {

    @Schema(description = "The unique identifier of the project", readOnly = true)
    private String id;

    @Schema(description = "The technical name of the project")
    private String technicalName;

    @Schema(description = "The display name of the project")
    private String displayName;

    @Schema(description = "The description of the project")
    private String description;

    @Schema(description = "The version of the project")
    private String version;

    public ProjectImpl() {}

    public ProjectImpl(String id, String technicalName) {
        this.id = id;
        this.technicalName = technicalName;
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
        return technicalName;
    }

    @Override
    public void setName(String technicalName) {
        this.technicalName = technicalName;
    }

    @Override
    public String getTechnicalName() {
        return technicalName;
    }

    @Override
    public void setTechnicalName(String technicalName) {
        this.technicalName = technicalName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String name) {
        this.displayName = name;
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
