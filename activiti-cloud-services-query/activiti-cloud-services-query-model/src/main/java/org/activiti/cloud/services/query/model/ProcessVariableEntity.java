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

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity(name="ProcessVariable")
@Table(name = "PROCESS_VARIABLE",
        indexes = {
                @Index(name = "proc_var_processInstanceId_idx", columnList = "processInstanceId", unique = false),
                @Index(name = "proc_var_name_idx", columnList = "name", unique = false),
                @Index(name = "proc_var_executionId_idx", columnList = "executionId", unique = false)
        })
public class ProcessVariableEntity extends AbstractVariableEntity {

    public ProcessVariableEntity() {
    }

    public ProcessVariableEntity(Long id,
                          String type,
                          String name,
                          String processInstanceId,
                          String serviceName,
                          String serviceFullName,
                          String serviceVersion,
                          String appName,
                          String appVersion,
                          Date createTime,
                          Date lastUpdatedTime,
                          String executionId) {
        super(id,
              type,
              name,
              processInstanceId,
              serviceName,
              serviceFullName,
              serviceVersion,
              appName,
              appVersion,
              createTime,
              lastUpdatedTime,
              executionId);
        
    }

    @Override
    public String getTaskId() {
        return null;
    }
    
    @Override
    public boolean isTaskVariable() {
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }

}