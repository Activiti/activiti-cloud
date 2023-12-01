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
import org.activiti.cloud.api.process.model.events.CloudProcessCancelledEvent;

public class CloudProcessCancelledEventImpl
    extends CloudProcessInstanceEventImpl
    implements CloudProcessCancelledEvent {

    private String cause;

    public CloudProcessCancelledEventImpl() {}

    public CloudProcessCancelledEventImpl(ProcessInstance processInstance) {
        super(processInstance);
        setEntityId(processInstance.getId());
    }

    public CloudProcessCancelledEventImpl(ProcessInstance processInstance, String cause) {
        super(processInstance);
        this.cause = cause;
    }

    public CloudProcessCancelledEventImpl(String id, Long timestamp, ProcessInstance processInstance) {
        super(id, timestamp, processInstance);
    }

    @Override
    public ProcessEvents getEventType() {
        return ProcessEvents.PROCESS_CANCELLED;
    }

    @Override
    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
