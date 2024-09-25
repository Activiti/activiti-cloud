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

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.impl.cmd.DeleteProcessInstanceCmd;
import org.activiti.engine.impl.interceptor.CommandContext;

public class DeleteCloudProcessInstanceCmd extends DeleteProcessInstanceCmd {

    private final ProcessEngineEventsAggregator processEngineEventsAggregator;
    private final ProcessInstance processInstance;

    public DeleteCloudProcessInstanceCmd(
        ProcessInstance processInstance,
        ProcessEngineEventsAggregator processEngineEventsAggregator
    ) {
        super(processInstance.getId(), null);
        this.processInstance = processInstance;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        super.execute(commandContext);

        processEngineEventsAggregator.add(new CloudProcessDeletedEventImpl(processInstance));

        return null;
    }
}
