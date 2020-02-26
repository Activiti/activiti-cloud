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

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.payloads.ClaimTaskPayload;
import org.activiti.api.task.model.results.TaskResult;
import org.activiti.api.task.runtime.TaskAdminRuntime;

public class ClaimTaskCmdExecutor extends AbstractCommandExecutor<ClaimTaskPayload> {

    private TaskAdminRuntime taskAdminRuntime;

    public ClaimTaskCmdExecutor(TaskAdminRuntime taskAdminRuntime) {
        this.taskAdminRuntime = taskAdminRuntime;
    }

    @Override
    public TaskResult execute(ClaimTaskPayload claimTaskPayload) {
        Task task = taskAdminRuntime.claim(claimTaskPayload);
        
        return new TaskResult(claimTaskPayload, task);
    }
}
