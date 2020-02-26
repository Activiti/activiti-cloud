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

package org.activiti.cloud.services.query.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.activiti.cloud.api.process.model.CloudProcessDefinition;

@Entity(name = "ProcessDefinition")
@Table(name = "PROCESS_DEFINITION",
        indexes = {
                @Index(name = "pd_name_idx", columnList = "name"),
                @Index(name = "pd_key_idx", columnList = "processDefinitionKey")
        })
public class ProcessDefinitionEntity extends ActivitiEntityMetadata implements CloudProcessDefinition {

    @Id
    private String id;
    private String name;
    @Column(name = "processDefinitionKey")
    private String key;
    private String description;
    private int version;
    private String formKey;

    public ProcessDefinitionEntity() {
    }

    public ProcessDefinitionEntity(String serviceName,
                                   String serviceFullName,
                                   String serviceVersion,
                                   String appName,
                                   String appVersion) {
        super(serviceName,
              serviceFullName,
              serviceVersion,
              appName,
              appVersion);
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(id);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProcessDefinitionEntity other = (ProcessDefinitionEntity) obj;
        return Objects.equals(id, other.id);
    }
}
