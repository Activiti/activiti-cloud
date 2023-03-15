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

import org.activiti.api.process.model.BPMNMessage;
import org.activiti.api.process.model.events.BPMNMessageEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;

public abstract class CloudBPMNMessageEventImpl
    extends CloudRuntimeEventImpl<BPMNMessage, BPMNMessageEvent.MessageEvents> {

    public CloudBPMNMessageEventImpl() {}

    public CloudBPMNMessageEventImpl(BPMNMessage entity, String processDefinitionId, String processInstanceId) {
        super(entity);
        setProcessDefinitionId(processDefinitionId);
        setProcessInstanceId(processInstanceId);
        setEntityId(entity.getElementId());
    }

    public CloudBPMNMessageEventImpl(
        String id,
        Long timestamp,
        BPMNMessage entity,
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
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return true;
    }
}
