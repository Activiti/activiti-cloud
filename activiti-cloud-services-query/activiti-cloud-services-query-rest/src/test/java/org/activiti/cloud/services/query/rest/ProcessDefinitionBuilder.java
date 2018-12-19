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

package org.activiti.cloud.services.query.rest;

import java.util.UUID;

import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;

public class ProcessDefinitionBuilder {

    public static ProcessDefinitionEntity buildDefaultProcessDefinition() {
        ProcessDefinitionEntity processDefinitionEntity = new ProcessDefinitionEntity();
        processDefinitionEntity.setId(UUID.randomUUID().toString());
        processDefinitionEntity.setName("My Process");
        processDefinitionEntity.setKey("myProcess");
        processDefinitionEntity.setVersion(1);
        processDefinitionEntity.setDescription("This is my process description");
        return processDefinitionEntity;
    }

}
