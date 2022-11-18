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

import org.activiti.api.process.model.BPMNError;
import org.activiti.api.process.model.events.BPMNErrorReceivedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;

public class CloudBPMNErrorReceivedEventImpl
    extends CloudRuntimeEventImpl<BPMNError, BPMNErrorReceivedEvent.ErrorEvents>
    implements CloudBPMNErrorReceivedEvent {

    public CloudBPMNErrorReceivedEventImpl() {}

    public CloudBPMNErrorReceivedEventImpl(BPMNError entity, String processDefinitionId, String processInstanceId) {
        super(entity);
        setProcessDefinitionId(processDefinitionId);
        setProcessInstanceId(processInstanceId);

        if (entity != null) {
            setEntityId(entity.getElementId());
        }
    }

    public CloudBPMNErrorReceivedEventImpl(
        String id,
        Long timestamp,
        BPMNError entity,
        String processDefinitionId,
        String processInstanceId
    ) {
        super(id, timestamp, entity);
        setProcessDefinitionId(processDefinitionId);
        setProcessInstanceId(processInstanceId);

        if (entity != null) {
            setEntityId(entity.getElementId());
        }
    }

    @Override
    public BPMNErrorReceivedEvent.ErrorEvents getEventType() {
        return BPMNErrorReceivedEvent.ErrorEvents.ERROR_RECEIVED;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("CloudBPMNErrorReceivedEventImpl [getEventType()=")
            .append(getEventType())
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
