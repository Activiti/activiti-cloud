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

import org.activiti.api.process.model.BPMNTimer;
import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerCancelledEvent;

public class CloudBPMNTimerCancelledEventImpl extends CloudBPMNTimerEventImpl implements CloudBPMNTimerCancelledEvent {

    public CloudBPMNTimerCancelledEventImpl() {
    }

    public CloudBPMNTimerCancelledEventImpl(BPMNTimer entity,
                                            String processDefinitionId,
                                            String processInstanceId) {
        super(entity,
              processDefinitionId,
              processInstanceId);
    }

    public CloudBPMNTimerCancelledEventImpl(String id,
                                            Long timestamp,
                                            BPMNTimer entity,
                                            String processDefinitionId,
                                            String processInstanceId) {
        super(id,
              timestamp,
              entity,
              processDefinitionId,
              processInstanceId);
    }

    @Override
    public BPMNTimerEvent.TimerEvents getEventType() {
        return BPMNTimerEvent.TimerEvents.TIMER_CANCELLED;
    }
}
