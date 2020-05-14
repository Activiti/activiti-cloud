/*
 * Copyright 2017-2020 Alfresco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.api.process.model.impl.events;

import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCancelledEvent;

public class CloudBPMNActivityCancelledEventImpl extends CloudBPMNActivityEventImpl implements CloudBPMNActivityCancelledEvent {

    private String cause;

    public CloudBPMNActivityCancelledEventImpl() {
    }

    public CloudBPMNActivityCancelledEventImpl(BPMNActivity entity,
                                               String processDefinitionId,
                                               String processInstanceId,
                                               String cause) {
        super(entity,
              processDefinitionId,
              processInstanceId);
        this.cause = cause;
    }

    public CloudBPMNActivityCancelledEventImpl(String id,
                                               Long timestamp,
                                               BPMNActivity entity,
                                               String processDefinitionId,
                                               String processInstanceId,
                                               String cause) {
        super(id,
              timestamp,
              entity,
              processDefinitionId,
              processInstanceId);
        this.cause = cause;
    }

    @Override
    public BPMNActivityEvent.ActivityEvents getEventType() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED;
    }

    @Override
    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
