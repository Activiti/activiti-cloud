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
package org.activiti.cloud.services.events.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.conf.IgnoredRuntimeEvent;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class ExecutionContextMessageBuilderAppenderTest {

    private static final String MOCK_SUPER_EXECUTION_ID = "mockSuperExectuionId";
    private static final String MOCK_PROCESS_DEFINITION_NAME = "mockProcessDefinitionName";
    private static final String MOCK_PARENT_PROCESS_NAME = "mockParentProcessName";
    private static final String MOCK_PROCESS_NAME = "mockProcessName";
    private static final String MOCK_DEPLOYMENT_NAME = "mockDeploymentName";
    private static final String MOCK_DEPLOYMENT_ID = "mockDeploymentId";
    private static final int MOCK_PROCESS_DEFINITION_VERSION = 0;
    private static final String MOCK_PROCESS_DEFINITION_KEY = "mockProcessDefinitionKey";
    private static final String MOCK_PROCESS_DEFINITION_ID = "mockProcessDefinitionId";
    private static final String MOCK_PARENT_PROCESS_INSTANCE_ID = "mockParentId";
    private static final String MOCK_PROCESS_INSTANCE_ID = "mockProcessInstanceId";
    private static final String MOCK_BUSINESS_KEY = "mockBusinessKey";
    private static final int MOCK_APP_VERSION = 1;

    private ExecutionContextMessageBuilderAppender subject;

    @BeforeEach
    public void setUp() {
        ExecutionContext context = mockExecutionContext();

        subject = new ExecutionContextMessageBuilderAppender(context);
    }

    @Test
    public void testApply() {
        // given
        MessageBuilder<CloudRuntimeEvent<?, ?>> request = MessageBuilder.withPayload(new IgnoredRuntimeEvent());

        // when
        subject.apply(request);

        // then
        Message<CloudRuntimeEvent<?, ?>> message = request.build();

        assertThat(message.getHeaders())
            .containsEntry(ExecutionContextMessageHeaders.ROOT_BUSINESS_KEY, MOCK_BUSINESS_KEY)
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_INSTANCE_ID, MOCK_PROCESS_INSTANCE_ID)
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_DEFINITION_ID, MOCK_PROCESS_DEFINITION_ID)
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_DEFINITION_KEY, MOCK_PROCESS_DEFINITION_KEY)
            .containsEntry(ExecutionContextMessageHeaders.PARENT_PROCESS_INSTANCE_ID, MOCK_PARENT_PROCESS_INSTANCE_ID)
            .containsEntry(
                ExecutionContextMessageHeaders.ROOT_PROCESS_DEFINITION_VERSION,
                MOCK_PROCESS_DEFINITION_VERSION
            )
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_NAME, MOCK_PROCESS_NAME)
            .containsEntry(ExecutionContextMessageHeaders.PARENT_PROCESS_INSTANCE_NAME, MOCK_PARENT_PROCESS_NAME)
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_DEFINITION_NAME, MOCK_PROCESS_DEFINITION_NAME)
            .containsEntry(ExecutionContextMessageHeaders.DEPLOYMENT_ID, MOCK_DEPLOYMENT_ID)
            .containsEntry(ExecutionContextMessageHeaders.DEPLOYMENT_NAME, MOCK_DEPLOYMENT_NAME)
            .containsEntry(ExecutionContextMessageHeaders.DEPLOYMENT_VERSION, MOCK_APP_VERSION);
    }

    private ExecutionContext mockExecutionContext() {
        ExecutionContext context = mock(ExecutionContext.class);
        ExecutionEntity processInstance = mock(ExecutionEntity.class);
        DeploymentEntity deploymentEntity = mock(DeploymentEntity.class);
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);

        when(context.getProcessInstance()).thenReturn(processInstance);
        when(context.getDeployment()).thenReturn(deploymentEntity);
        when(context.getProcessDefinition()).thenReturn(processDefinition);

        when(processInstance.getId()).thenReturn(MOCK_PROCESS_INSTANCE_ID);
        when(processInstance.getBusinessKey()).thenReturn(MOCK_BUSINESS_KEY);
        when(processInstance.getName()).thenReturn(MOCK_PROCESS_NAME);

        ExecutionEntity superExectuion = mock(ExecutionEntity.class);
        when(processInstance.getSuperExecutionId()).thenReturn(MOCK_SUPER_EXECUTION_ID);
        when(processInstance.getSuperExecution()).thenReturn(superExectuion);

        ExecutionEntity parentProcessInstance = mock(ExecutionEntity.class);
        when(superExectuion.getProcessInstanceId()).thenReturn(MOCK_PARENT_PROCESS_INSTANCE_ID);
        when(superExectuion.getProcessInstance()).thenReturn(parentProcessInstance);
        when(parentProcessInstance.getId()).thenReturn(MOCK_PARENT_PROCESS_INSTANCE_ID);
        when(parentProcessInstance.getName()).thenReturn(MOCK_PARENT_PROCESS_NAME);

        when(processDefinition.getId()).thenReturn(MOCK_PROCESS_DEFINITION_ID);
        when(processDefinition.getKey()).thenReturn(MOCK_PROCESS_DEFINITION_KEY);
        when(processDefinition.getVersion()).thenReturn(MOCK_PROCESS_DEFINITION_VERSION);
        when(processDefinition.getName()).thenReturn(MOCK_PROCESS_DEFINITION_NAME);

        when(deploymentEntity.getId()).thenReturn(MOCK_DEPLOYMENT_ID);
        when(deploymentEntity.getName()).thenReturn(MOCK_DEPLOYMENT_NAME);
        when(deploymentEntity.getVersion()).thenReturn(MOCK_APP_VERSION);

        return context;
    }
}
