/*
 * Copyright 2010-2020 Alfresco Software, Ltd.
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

package org.activiti.services.connectors.channel;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.services.connectors.behavior.MQServiceTaskBehavior;

import java.util.List;

public class IntegrationRequestReplayer {

    private final RuntimeService runtimeService;
    private final ManagementService managementService;
    private final MQServiceTaskBehavior mqServiceTaskBehavior;

    public IntegrationRequestReplayer(RuntimeService runtimeService,
                                      ManagementService managementService,
                                      MQServiceTaskBehavior mqServiceTaskBehavior) {
        this.runtimeService = runtimeService;
        this.managementService = managementService;
        this.mqServiceTaskBehavior = mqServiceTaskBehavior;
    }

    public void replay(String executionId, String flowNodeId) {
        List<Execution> executions = runtimeService.createExecutionQuery()
                                                   .executionId(executionId)
                                                   .list();
        if (executions.size() > 0) {
            ExecutionEntity execution = ExecutionEntity.class.cast(executions.get(0));
            if (execution.getActivityId().equals(flowNodeId)) {
                managementService.executeCommand((Command<Void>) commandContext -> {
                    mqServiceTaskBehavior.execute(execution);
                    return null;
                });
            } else {
                throw new ActivitiException("Unable to replay integration context because it points to flowNode '" +
                    flowNodeId + "' while the related execution points to flowNode '" + execution.getActivityId() );
            }
        } else {
            String message = "Unable to replay integration request because no task is in this RB is waiting for integration result with execution id `" +
                executionId + ", flow node id `" + flowNodeId + "'";
            throw new ActivitiException(message);
        }
    }
}
