/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.EmptyResult;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.payloads.DeleteProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;

public class DeleteProcessInstanceCmdExecutor extends AbstractCommandExecutor<DeleteProcessPayload> {

    private ProcessAdminRuntime processAdminRuntime;

    public DeleteProcessInstanceCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        this.processAdminRuntime = processAdminRuntime;
    }

    @Override
    public EmptyResult execute(DeleteProcessPayload deleteProcessPayload) {
        ProcessInstance processInstance = processAdminRuntime.delete(deleteProcessPayload);
        if (processInstance != null) {
            return new EmptyResult(deleteProcessPayload);
        } else {
            throw new IllegalStateException("Failed to delete processInstance");
        }
    }
}
