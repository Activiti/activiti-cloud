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
package org.activiti.cloud.services.query.app.repository.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;

public class ProcessInstanceHelper {

    private ProcessInstanceHelper() {}

    public static ProcessInstanceEntity createProcessInstance(String parentId) {
        ProcessInstanceEntity entity = new ProcessInstanceEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(UUID.randomUUID().toString());
        entity.setProcessDefinitionName("process-definition");
        entity.setParentId(parentId.equals("1") ? null : parentId);
        return entity;
    }

    public static List<ProcessInstanceEntity> createParentProcessInstances(int count) {
        List<ProcessInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            instances.add(createProcessInstance("1"));
        }
        return instances;
    }

    public static List<ProcessInstanceEntity> createSubprocessInstances(int count, String parentId) {
        List<ProcessInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            instances.add(createProcessInstance(parentId));
        }
        return instances;
    }
}
