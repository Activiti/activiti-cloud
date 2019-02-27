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

package org.activiti.cloud.api.process.model.impl.events;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;

public class CloudProcessDeployedEventImpl extends CloudRuntimeEventImpl<ProcessDefinition, ProcessDefinitionEvent.ProcessDefinitionEvents> implements CloudProcessDeployedEvent {

    private String processModelContent;

    public CloudProcessDeployedEventImpl() {
    }

    public CloudProcessDeployedEventImpl(ProcessDefinition entity) {
        super(entity);
        setEntityId(entity.getId());
    }
    public CloudProcessDeployedEventImpl(String id,
                                         Long timestamp,
                                         ProcessDefinition entity) {
        super(id, timestamp, entity);
        setEntityId(entity.getId());
    }

    @Override
    public ProcessDefinitionEvents getEventType() {
        return ProcessDefinitionEvents.PROCESS_DEPLOYED;
    }

    @Override
    public String getProcessModelContent() {
        return processModelContent;
    }

    public void setProcessModelContent(String processModelContent) {
        this.processModelContent = processModelContent;
    }

}
