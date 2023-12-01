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

import org.activiti.api.process.model.BPMNSignal;
import org.activiti.api.process.model.events.BPMNSignalEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;

public class CloudBPMNSignalReceivedEventImpl extends CloudBPMNSignalEventImpl implements CloudBPMNSignalReceivedEvent {

    public CloudBPMNSignalReceivedEventImpl() {}

    public CloudBPMNSignalReceivedEventImpl(BPMNSignal entity, String processDefinitionId, String processInstanceId) {
        super(entity, processDefinitionId, processInstanceId);
    }

    public CloudBPMNSignalReceivedEventImpl(
        String id,
        Long timestamp,
        BPMNSignal entity,
        String processDefinitionId,
        String processInstanceId
    ) {
        super(id, timestamp, entity, processDefinitionId, processInstanceId);
    }

    @Override
    public BPMNSignalEvent.SignalEvents getEventType() {
        return BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED;
    }
}
