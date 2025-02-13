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

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.process.model.payloads.ResumeProcessPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.SuspendProcessPayload;
import org.activiti.api.task.model.payloads.ClaimTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.ReleaseTaskPayload;
import org.activiti.cloud.api.process.model.impl.SyncCloudProcessDefinitionsPayload;
import org.activiti.cloud.api.process.model.impl.SyncCloudProcessDefinitionsResult;
import org.activiti.cloud.services.gateway.ProcessRuntimeGateway;
import org.springframework.beans.factory.annotation.Autowired;

public class ProcessRuntimeGatewayIT extends CommandEndpointIT {

    @Autowired
    private ProcessRuntimeGateway processRuntimeGateway;

    @Override
    protected SyncCloudProcessDefinitionsResult doSyncCloudProcessDefinitions(
        SyncCloudProcessDefinitionsPayload payload
    ) {
        return processRuntimeGateway.syncProcessDefinitions(payload);
    }

    @Override
    protected void doSendSignal(SignalPayload payload) {
        var result = processRuntimeGateway.sendSignal(payload);

        assertThat(result).isNotNull();
    }

    @Override
    protected void doClaimTask(ClaimTaskPayload payload) {
        var result = processRuntimeGateway.claimTask(payload);

        assertThat(result).isNotNull();
    }

    @Override
    protected void doCompleteTask(CompleteTaskPayload payload) {
        var result = processRuntimeGateway.completeTask(payload);

        assertThat(result).isNotNull();
    }

    @Override
    protected void doResumeProcessInstance(ResumeProcessPayload payload) {
        var result = processRuntimeGateway.resumeProcess(payload);

        assertThat(result).isNotNull();
    }

    @Override
    protected void doSuspendProcessInstance(SuspendProcessPayload payload) {
        var result = processRuntimeGateway.suspendProcess(payload);

        assertThat(result).isNotNull();
    }

    @Override
    protected String doStartProcessInstance(StartProcessPayload payload) {
        var result = processRuntimeGateway.startProcess(payload);

        assertThat(result).isNotNull();

        return result.getEntity().getId();
    }

    @Override
    protected void doReleaseTask(ReleaseTaskPayload payload) {
        var result = processRuntimeGateway.releaseTask(payload);

        assertThat(result).isNotNull();
    }

    @Override
    protected void doSetProcessVariables(SetProcessVariablesPayload payload) {
        var result = processRuntimeGateway.setProcessVariables(payload);

        assertThat(result).isNotNull();
    }
}
