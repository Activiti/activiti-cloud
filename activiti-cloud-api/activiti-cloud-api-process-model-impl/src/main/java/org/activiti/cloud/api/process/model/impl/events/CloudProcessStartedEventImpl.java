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
package org.activiti.cloud.api.process.model.impl.events;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;

public class CloudProcessStartedEventImpl extends CloudProcessInstanceEventImpl implements CloudProcessStartedEvent {

    private String nestedProcessDefinitionId;
    private String nestedProcessInstanceId;

    public CloudProcessStartedEventImpl() {}

    public CloudProcessStartedEventImpl(
        ProcessInstance processInstance,
        String nestedProcessDefinitionId,
        String nestedProcessInstanceId
    ) {
        super(processInstance);
        this.nestedProcessDefinitionId = nestedProcessDefinitionId;
        this.nestedProcessInstanceId = nestedProcessInstanceId;
    }

    public CloudProcessStartedEventImpl(ProcessInstance processInstance) {
        this(processInstance, null, null);
    }

    public CloudProcessStartedEventImpl(String id, Long timestamp, ProcessInstance processInstance) {
        super(id, timestamp, processInstance);
    }

    public CloudProcessStartedEventImpl(
        String id,
        Long timestamp,
        ProcessInstance processInstance,
        String nestedProcessDefinitionId,
        String nestedProcessInstanceId
    ) {
        super(id, timestamp, processInstance);
        this.nestedProcessDefinitionId = nestedProcessDefinitionId;
        this.nestedProcessInstanceId = nestedProcessInstanceId;
    }

    @Override
    public String getNestedProcessInstanceId() {
        return nestedProcessInstanceId;
    }

    @Override
    public String getNestedProcessDefinitionId() {
        return nestedProcessDefinitionId;
    }

    @Override
    public ProcessRuntimeEvent.ProcessEvents getEventType() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED;
    }
}
