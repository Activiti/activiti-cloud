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

import org.activiti.api.process.model.ProcessCandidateStarterGroup;
import org.activiti.api.process.model.events.ProcessCandidateStarterGroupEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterGroupAddedEvent;

public class CloudProcessCandidateStarterGroupAddedEventImpl
    extends CloudRuntimeEventImpl<ProcessCandidateStarterGroup, ProcessCandidateStarterGroupEvent.ProcessCandidateStarterGroupEvents>
    implements CloudProcessCandidateStarterGroupAddedEvent {

    public CloudProcessCandidateStarterGroupAddedEventImpl() {}

    public CloudProcessCandidateStarterGroupAddedEventImpl(ProcessCandidateStarterGroup processCandidateStarterGroup) {
        super(processCandidateStarterGroup);
    }

    public CloudProcessCandidateStarterGroupAddedEventImpl(
        String id,
        Long timestamp,
        ProcessCandidateStarterGroup processCandidateStarterGroup
    ) {
        super(id, timestamp, processCandidateStarterGroup);
        setEntityId(processCandidateStarterGroup.getGroupId());
    }

    @Override
    public ProcessCandidateStarterGroupEvents getEventType() {
        return ProcessCandidateStarterGroupEvents.PROCESS_CANDIDATE_STARTER_GROUP_ADDED;
    }
}
