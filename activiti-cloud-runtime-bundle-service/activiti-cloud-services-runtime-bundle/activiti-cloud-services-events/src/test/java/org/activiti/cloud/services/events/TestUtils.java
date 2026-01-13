/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.events;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.persistence.entity.DeploymentEntityImpl;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;

public class TestUtils {

    public static final String MOCK_PROCESS_INSTANCE_ID = "mockProcessInstanceId";
    public static final String MOCK_BUSINESS_KEY = "mockBusinessKey";
    public static final String MOCK_PROCESS_NAME = "mockProcessName";
    public static final String MOCK_SUPER_EXECUTION_ID = "mockSuperExecutionId";
    public static final String MOCK_PARENT_PROCESS_INSTANCE_ID = "mockParentId";
    public static final String MOCK_PARENT_PROCESS_NAME = "mockParentProcessName";
    public static final String MOCK_PROCESS_DEFINITION_ID = "mockProcessDefinitionId";
    public static final String MOCK_PROCESS_DEFINITION_KEY = "mockProcessDefinitionKey";
    public static final Integer MOCK_PROCESS_DEFINITION_VERSION = 0;
    public static final String MOCK_PROCESS_DEFINITION_NAME = "mockProcessDefinitionName";
    public static final String MOCK_DEPLOYMENT_ID = "mockDeploymentId";
    public static final String MOCK_DEPLOYMENT_NAME = "mockDeploymentName";
    public static final int MOCK_APP_VERSION = 1;

    private TestUtils() {}

    public static ExecutionContext mockExecutionContext() {
        return mockExecutionContext(
            MOCK_PROCESS_INSTANCE_ID,
            MOCK_BUSINESS_KEY,
            MOCK_PROCESS_NAME,
            MOCK_SUPER_EXECUTION_ID,
            MOCK_PARENT_PROCESS_INSTANCE_ID,
            MOCK_PARENT_PROCESS_NAME,
            MOCK_PROCESS_DEFINITION_ID,
            MOCK_PROCESS_DEFINITION_KEY,
            MOCK_PROCESS_DEFINITION_VERSION,
            MOCK_PROCESS_DEFINITION_NAME,
            MOCK_DEPLOYMENT_ID,
            MOCK_DEPLOYMENT_NAME,
            MOCK_APP_VERSION
        );
    }

    public static ExecutionContext mockExecutionContext(
        String processInstanceId,
        String businessKey,
        String processName,
        String superExecutionId,
        String parentProcessInstanceId,
        String parentProcessName,
        String processDefinitionId,
        String processDefinitionKey,
        Integer processDefinitionVersion,
        String processDefinitionName,
        String deploymentId,
        String deploymentName,
        Integer appVersion
    ) {
        ExecutionEntityImpl processInstance = new ExecutionEntityImpl();
        processInstance.setId(processInstanceId);
        processInstance.setBusinessKey(businessKey);
        processInstance.setName(processName);

        ExecutionEntityImpl parentProcessInstance = new ExecutionEntityImpl();
        parentProcessInstance.setId(parentProcessInstanceId);
        parentProcessInstance.setName(parentProcessName);

        ExecutionEntity superExecution = mock(ExecutionEntity.class);
        when(superExecution.getProcessInstanceId()).thenReturn(parentProcessInstanceId);
        when(superExecution.getProcessInstance()).thenReturn(parentProcessInstance);

        ExecutionEntity mockedProcessInstance = mock(ExecutionEntity.class);
        when(mockedProcessInstance.getId()).thenReturn(processInstanceId);
        when(mockedProcessInstance.getBusinessKey()).thenReturn(businessKey);
        when(mockedProcessInstance.getName()).thenReturn(processName);
        when(mockedProcessInstance.getSuperExecutionId()).thenReturn(superExecutionId);
        when(mockedProcessInstance.getSuperExecution()).thenReturn(superExecution);

        ProcessDefinitionEntityImpl processDefinition = new ProcessDefinitionEntityImpl();
        processDefinition.setId(processDefinitionId);
        processDefinition.setKey(processDefinitionKey);
        processDefinition.setVersion(processDefinitionVersion);
        processDefinition.setName(processDefinitionName);

        DeploymentEntityImpl deploymentEntity = new DeploymentEntityImpl();
        deploymentEntity.setId(deploymentId);
        deploymentEntity.setName(deploymentName);
        deploymentEntity.setVersion(appVersion);

        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getProcessInstance()).thenReturn(mockedProcessInstance);
        when(context.getDeployment()).thenReturn(deploymentEntity);
        when(context.getProcessDefinition()).thenReturn(processDefinition);
        when(context.getExecution()).thenReturn(mockedProcessInstance);

        return context;
    }
}
