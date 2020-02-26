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
package org.activiti.cloud.services.events.message;

import java.util.Optional;

import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.lang.Nullable;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

public class ExecutionContextMessageBuilderAppender implements MessageBuilderAppender {

    private final ExecutionContext executionContext;

    public ExecutionContextMessageBuilderAppender(@Nullable ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public <P> MessageBuilder<P> apply(MessageBuilder<P> request) {
        Assert.notNull(request, "request must not be null");
        
        if(executionContext != null) {
            ExecutionEntity processInstance = executionContext.getProcessInstance();
            ProcessDefinition processDefinition = executionContext.getProcessDefinition();
            DeploymentEntity deploymentEntity = executionContext.getDeployment();



            if(processInstance != null) { 
                request.setHeader(ExecutionContextMessageHeaders.BUSINESS_KEY, processInstance.getBusinessKey())
                    .setHeader(ExecutionContextMessageHeaders.PROCESS_INSTANCE_ID, processInstance.getId())
                    .setHeader(ExecutionContextMessageHeaders.PROCESS_NAME, processInstance.getName());
                
                // Let's try extract parent process info from super execution if exists
                applyParent(processInstance, request);
            }

            if(processDefinition != null) { 
                request.setHeader(ExecutionContextMessageHeaders.PROCESS_DEFINITION_ID, processDefinition.getId())
                    .setHeader(ExecutionContextMessageHeaders.PROCESS_DEFINITION_KEY, processDefinition.getKey())
                    .setHeader(ExecutionContextMessageHeaders.PROCESS_DEFINITION_VERSION, processDefinition.getVersion())
                    .setHeader(ExecutionContextMessageHeaders.PROCESS_DEFINITION_NAME, processDefinition.getName());
            }

            if(deploymentEntity != null) {
                 request.setHeader(ExecutionContextMessageHeaders.DEPLOYMENT_ID, deploymentEntity.getId())
                        .setHeader(ExecutionContextMessageHeaders.DEPLOYMENT_NAME, deploymentEntity.getName())
                        .setHeader(ExecutionContextMessageHeaders.APP_VERSION, deploymentEntity.getVersion());
            }
            
        }

        return request;

    }
    
    protected <P> MessageBuilder<P> applyParent(ExecutionEntity processInstance, MessageBuilder<P> request) {
        // Let's do it lazy way
        if (processInstance.getSuperExecutionId() != null) {
            Optional.ofNullable(processInstance.getSuperExecution())
                .ifPresent(superExecution -> {
                    request.setHeader(ExecutionContextMessageHeaders.PARENT_PROCESS_INSTANCE_ID, superExecution.getProcessInstanceId());
                    
                    // Let's do it lazy way
                    if(superExecution.getProcessInstanceId() != null) { 
                        Optional.ofNullable(superExecution.getProcessInstance())
                            .ifPresent(parentInstance -> {
                                request.setHeader(ExecutionContextMessageHeaders.PARENT_PROCESS_INSTANCE_NAME, parentInstance.getName());                                
                            });
                    }
                });
        }
        
        return request;
    }
    

}
