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

import org.activiti.api.process.model.BPMNSequenceFlow;
import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudSequenceFlowTakenEvent;

public class CloudSequenceFlowTakenEventImpl
    extends CloudRuntimeEventImpl<BPMNSequenceFlow, SequenceFlowEvent.SequenceFlowEvents>
    implements CloudSequenceFlowTakenEvent {

    public CloudSequenceFlowTakenEventImpl() {}

    public CloudSequenceFlowTakenEventImpl(BPMNSequenceFlow sequenceFlow) {
        super(sequenceFlow);
        if (getEntity() != null) {
            setEntityId(getEntity().getElementId());
        }
    }

    public CloudSequenceFlowTakenEventImpl(String id, Long timestamp, BPMNSequenceFlow entity) {
        super(id, timestamp, entity);
        if (getEntity() != null) {
            setEntityId(getEntity().getElementId());
        }
    }

    @Override
    public SequenceFlowEvent.SequenceFlowEvents getEventType() {
        return SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN;
    }
}
