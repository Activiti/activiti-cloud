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
package org.activiti.cloud.api.process.model.impl;

import java.util.ArrayList;
import java.util.List;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.VariableDefinition;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;

public class CloudProcessDefinitionImpl extends CloudRuntimeEntityImpl implements ExtendedCloudProcessDefinition {

    private String id;
    private String name;
    private String key;
    private String description;
    private int version;
    private String formKey;
    private String category;
    private List<VariableDefinition> variableDefinitions = new ArrayList<>();

    public CloudProcessDefinitionImpl() {}

    public CloudProcessDefinitionImpl(ProcessDefinition processDefinition) {
        super(processDefinition);
        id = processDefinition.getId();
        name = processDefinition.getName();
        key = processDefinition.getKey();
        description = processDefinition.getDescription();
        version = processDefinition.getVersion();
        formKey = processDefinition.getFormKey();
        category = processDefinition.getCategory();
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public List<VariableDefinition> getVariableDefinitions() {
        return variableDefinitions;
    }

    public void setVariableDefinitions(List<VariableDefinition> variableDefinitions) {
        this.variableDefinitions = variableDefinitions;
    }
}
