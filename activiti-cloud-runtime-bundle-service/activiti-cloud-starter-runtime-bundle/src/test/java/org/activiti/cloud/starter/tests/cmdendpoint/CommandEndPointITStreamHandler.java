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
package org.activiti.cloud.starter.tests.cmdendpoint;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.activiti.api.model.shared.Result;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.ResumeProcessPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.SuspendProcessPayload;
import org.activiti.api.task.model.payloads.ClaimTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.ReleaseTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

@Profile(CommandEndPointITStreamHandler.COMMAND_ENDPOINT_IT)
@TestComponent
public class CommandEndPointITStreamHandler {

    public static final String COMMAND_ENDPOINT_IT = "CommandEndpointIT";

    private String processInstanceId;

    private AtomicBoolean startedProcessInstanceAck = new AtomicBoolean(false);
    private AtomicBoolean suspendedProcessInstanceAck = new AtomicBoolean(false);
    private AtomicBoolean resumedProcessInstanceAck = new AtomicBoolean(false);
    private AtomicBoolean claimedTaskAck = new AtomicBoolean(false);
    private AtomicBoolean releasedTaskAck = new AtomicBoolean(false);
    private AtomicBoolean completedTaskAck = new AtomicBoolean(false);
    private AtomicBoolean sendSignalAck = new AtomicBoolean(false);
    private AtomicBoolean setProcessVariablesAck = new AtomicBoolean(false);
    private AtomicBoolean removeProcessVariablesAck = new AtomicBoolean(false);
    private AtomicBoolean createTaskVariableAck = new AtomicBoolean(false);
    private AtomicBoolean updateTaskVariableAck = new AtomicBoolean(false);

    @TestConfiguration
    class CommandEndpointITStreamHandlerConfiguration {

        @FunctionBinding(input = MessageClientStream.MY_CMD_RESULTS)
        @Bean
        public Consumer<Message<Result<?>>> consumeStartProcessInstanceResults() {
            return message -> {
                Result<?> result = message.getPayload();
                if (result.getPayload() instanceof StartProcessPayload) {
                    assertThat(result.getEntity()).isNotNull();
                    assertThat(result.getEntity()).isInstanceOf(ProcessInstance.class);
                    assertThat(((ProcessInstance) result.getEntity()).getId()).isNotEmpty();
                    processInstanceId = ((ProcessInstance) result.getEntity()).getId();
                    startedProcessInstanceAck.set(true);
                } else if (result.getPayload() instanceof SuspendProcessPayload) {
                    suspendedProcessInstanceAck.set(true);
                } else if (result.getPayload() instanceof ResumeProcessPayload) {
                    resumedProcessInstanceAck.set(true);
                } else if (result.getPayload() instanceof ClaimTaskPayload) {
                    claimedTaskAck.set(true);
                } else if (result.getPayload() instanceof ReleaseTaskPayload) {
                    releasedTaskAck.set(true);
                } else if (result.getPayload() instanceof CompleteTaskPayload) {
                    completedTaskAck.set(true);
                } else if (result.getPayload() instanceof SignalPayload) {
                    sendSignalAck.set(true);
                } else if (result.getPayload() instanceof CreateTaskVariablePayload) {
                    createTaskVariableAck.set(true);
                } else if (result.getPayload() instanceof UpdateTaskVariablePayload) {
                    updateTaskVariableAck.set(true);
                } else if (result.getPayload() instanceof SetProcessVariablesPayload) {
                    setProcessVariablesAck.set(true);
                } else if (result.getPayload() instanceof RemoveProcessVariablesPayload) {
                    removeProcessVariablesAck.set(true);
                }
            };
        };
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public AtomicBoolean getStartedProcessInstanceAck() {
        return startedProcessInstanceAck;
    }

    public AtomicBoolean getSuspendedProcessInstanceAck() {
        return suspendedProcessInstanceAck;
    }

    public AtomicBoolean getResumedProcessInstanceAck() {
        return resumedProcessInstanceAck;
    }

    public AtomicBoolean getClaimedTaskAck() {
        return claimedTaskAck;
    }

    public void resetClaimedTaskAck() {
        claimedTaskAck.set(false);
    }

    public AtomicBoolean getReleasedTaskAck() {
        return releasedTaskAck;
    }

    public AtomicBoolean getCompletedTaskAck() {
        return completedTaskAck;
    }

    public AtomicBoolean getSendSignalAck() {
        return sendSignalAck;
    }

    public AtomicBoolean getCreateTaskVariableAck() {
        return createTaskVariableAck;
    }

    public AtomicBoolean getUpdateTaskVariableAck() {
        return updateTaskVariableAck;
    }

    public AtomicBoolean getSetProcessVariablesAck() {
        return setProcessVariablesAck;
    }

    public AtomicBoolean getRemoveProcessVariablesAck() {
        return removeProcessVariablesAck;
    }
}
