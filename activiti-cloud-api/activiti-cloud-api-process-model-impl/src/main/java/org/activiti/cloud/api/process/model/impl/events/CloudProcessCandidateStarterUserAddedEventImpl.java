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

import org.activiti.api.process.model.ProcessCandidateStarterUser;
import org.activiti.api.process.model.events.ProcessCandidateStarterUserEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterUserAddedEvent;

public class CloudProcessCandidateStarterUserAddedEventImpl
    extends CloudRuntimeEventImpl<ProcessCandidateStarterUser, ProcessCandidateStarterUserEvent.ProcessCandidateStarterUserEvents>
    implements CloudProcessCandidateStarterUserAddedEvent {

    public CloudProcessCandidateStarterUserAddedEventImpl() {}

    public CloudProcessCandidateStarterUserAddedEventImpl(ProcessCandidateStarterUser processCandidateStarterUser) {
        super(processCandidateStarterUser);
    }

    public CloudProcessCandidateStarterUserAddedEventImpl(
        String id,
        Long timestamp,
        ProcessCandidateStarterUser processCandidateStarterUser
    ) {
        super(id, timestamp, processCandidateStarterUser);
        setEntityId(processCandidateStarterUser.getUserId());
    }

    @Override
    public ProcessCandidateStarterUserEvent.ProcessCandidateStarterUserEvents getEventType() {
        return ProcessCandidateStarterUserEvent.ProcessCandidateStarterUserEvents.PROCESS_CANDIDATE_STARTER_USER_ADDED;
    }
}
