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
package org.activiti.cloud.services.events.services;

import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.impl.cmd.DeleteProcessInstanceCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;

class DeleteCloudProcessInstanceCmd implements Command<Void> {

    private final List<Command<Void>> commands = new ArrayList<>();

    public DeleteCloudProcessInstanceCmd(
        String processInstanceId,
        ProcessEngineEventsAggregator processEngineEventsAggregator
    ) {
        commands.add(new DeleteProcessInstanceCmd(processInstanceId, null));
        commands.add(new SendDeleteCloudProcessInstanceEventCmd(processInstanceId, processEngineEventsAggregator));
    }

    @Override
    public Void execute(CommandContext commandContext) {
        commands.forEach(command -> command.execute(commandContext));

        return null;
    }
}
