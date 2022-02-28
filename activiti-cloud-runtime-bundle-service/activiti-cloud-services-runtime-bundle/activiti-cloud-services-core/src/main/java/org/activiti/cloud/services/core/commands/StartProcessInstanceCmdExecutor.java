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
package org.activiti.cloud.services.core.commands;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.results.ProcessInstanceResult;
import org.activiti.api.process.runtime.ProcessAdminRuntime;

public class StartProcessInstanceCmdExecutor
    extends AbstractCommandExecutor<StartProcessPayload> {

    private ProcessAdminRuntime processAdminRuntime;

    public StartProcessInstanceCmdExecutor(
        ProcessAdminRuntime processAdminRuntime
    ) {
        this.processAdminRuntime = processAdminRuntime;
    }

    @Override
    public ProcessInstanceResult execute(
        StartProcessPayload startProcessPayload
    ) {
        ProcessInstance processInstance = processAdminRuntime.start(
            startProcessPayload
        );
        if (processInstance != null) {
            return new ProcessInstanceResult(
                startProcessPayload,
                processInstance
            );
        } else {
            throw new IllegalStateException("Failed to start processInstance");
        }
    }
}
