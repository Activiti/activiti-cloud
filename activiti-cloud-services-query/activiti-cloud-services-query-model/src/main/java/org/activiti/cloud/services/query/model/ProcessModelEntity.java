/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity(name = "ProcessModel")
@Table(name = "PROCESS_MODEL")
public class ProcessModelEntity {

    @Id
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private ProcessDefinitionEntity processDefinition;

    @Column(columnDefinition="text")
    private String processModelContent;

    //used by persistence framework
    public ProcessModelEntity() {
    }

    public ProcessModelEntity(ProcessDefinitionEntity processDefinition,
                              String processModelContent) {
        this.processDefinition = processDefinition;
        this.processModelContent = processModelContent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProcessDefinitionEntity getProcessDefinition() {
        return processDefinition;
    }

    public String getProcessModelContent() {
        return processModelContent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProcessModelEntity other = (ProcessModelEntity) obj;
        return Objects.equals(id, other.id);
    }
}
