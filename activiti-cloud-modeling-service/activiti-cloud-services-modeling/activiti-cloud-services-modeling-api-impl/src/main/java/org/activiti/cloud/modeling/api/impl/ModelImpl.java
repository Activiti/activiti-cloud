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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.services.auditable.AbstractAuditable;
import org.springframework.util.StringUtils;

/**
 * Implementation for {@link Model}
 */
@Schema(name = "Model")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ModelImpl extends AbstractAuditable<String> implements Model<ProjectImpl, String> {

    @Schema(readOnly = true)
    private String id;

    @Schema(description = "The name of the model")
    private String name;

    @Schema(description = "The key (technical name) of the model")
    private String key;

    @Schema(description = "The type of the model", readOnly = true)
    private String type;

    @Schema(description = "The version of the model", readOnly = true)
    private String version;

    @Schema(description = "The content type of the model", readOnly = true, hidden = true)
    private String contentType;

    @Schema(description = "The content of the model", readOnly = true, hidden = true)
    private byte[] content;

    @Schema(hidden = true)
    @JsonIgnore
    private Set<ProjectImpl> projects = new HashSet<>();

    @Schema(description = "The extensions of the model", readOnly = true)
    private Map<String, Object> extensions;

    @Schema(description = "The template of the model", readOnly = true)
    private String template;

    @Schema(description = "The category of the model")
    private String category;

    @Schema(
        description = "The scope of the model. They can be shared between projects if it's scope is GLOBAL",
        readOnly = true
    )
    private ModelScope scope;

    public ModelImpl() {}

    public ModelImpl(String id, String name, String key, String type) {
        this.id = id;
        this.name = name;
        this.key = key;
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
    public String getKey() {
        if (StringUtils.hasText(key)) {
            return key;
        }
        return name;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
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
    public Set<ProjectImpl> getProjects() {
        return projects;
    }

    @Override
    public void addProject(ProjectImpl project) {
        if (project != null) {
            if (projects == null) {
                projects = new HashSet<>();
            }
            projects.add(project);
        }
    }

    @Override
    public void removeProject(ProjectImpl project) {
        if (projects != null) {
            projects.remove(project);
        }
    }

    @Override
    public void clearProjects() {
        projects.clear();
    }

    @Override
    public String getVersion() {
        return version;
    }

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
    public byte[] getContent() {
        return content;
    }

    @Override
    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
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
        return category;
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
    public String toString() {
        return type + " MODEL [" + id + ", " + name + "]";
    }
}
