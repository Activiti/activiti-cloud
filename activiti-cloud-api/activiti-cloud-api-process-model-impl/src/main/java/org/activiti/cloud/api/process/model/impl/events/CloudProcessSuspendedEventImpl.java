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
import org.activiti.cloud.api.process.model.events.CloudProcessSuspendedEvent;

public class CloudProcessSuspendedEventImpl
    extends CloudProcessInstanceEventImpl
    implements CloudProcessSuspendedEvent {

    public CloudProcessSuspendedEventImpl() {}

    public CloudProcessSuspendedEventImpl(ProcessInstance processInstance) {
        super(processInstance);
    }

    public CloudProcessSuspendedEventImpl(String id, Long timestamp, ProcessInstance processInstance) {
        super(id, timestamp, processInstance);
    }

    @Override
    public ProcessEvents getEventType() {
        return ProcessEvents.PROCESS_SUSPENDED;
    }
}
