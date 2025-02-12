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
package org.activiti.cloud.services.query.util;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;

public class ProcessInstanceTestUtils {

    private ProcessInstanceTestUtils() {}

    public static ProcessInstanceEntity buildProcessInstanceEntity() {
        return new ProcessInstanceEntity(
            "My-app",
            "My-app",
            "1",
            null,
            null,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            ProcessInstance.ProcessInstanceStatus.RUNNING,
            new Date()
        );
    }

    public static Set<ProcessVariableEntity> createProcessVariables(
        ProcessInstanceEntity processInstanceEntity,
        int numberOfVariables
    ) {
        Set<ProcessVariableEntity> variables = new HashSet<>();

        for (int i = 0; i < numberOfVariables; i++) {
            ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
            processVariableEntity.setName("name" + i);
            processVariableEntity.setValue("id");
            processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
            processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
            processVariableEntity.setProcessInstance(processInstanceEntity);
            variables.add(processVariableEntity);
        }
        return variables;
    }
}
