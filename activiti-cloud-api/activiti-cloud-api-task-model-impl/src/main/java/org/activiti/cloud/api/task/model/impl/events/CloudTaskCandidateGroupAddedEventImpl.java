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
package org.activiti.cloud.api.task.model.impl.events;

import org.activiti.api.task.model.TaskCandidateGroup;
import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupAddedEvent;

public class CloudTaskCandidateGroupAddedEventImpl
    extends CloudRuntimeEventImpl<TaskCandidateGroup, TaskCandidateGroupEvent.TaskCandidateGroupEvents>
    implements CloudTaskCandidateGroupAddedEvent {

    public CloudTaskCandidateGroupAddedEventImpl() {}

    public CloudTaskCandidateGroupAddedEventImpl(TaskCandidateGroup taskCandidateGroup) {
        super(taskCandidateGroup);
        setEntityId(taskCandidateGroup.getGroupId());
    }

    public CloudTaskCandidateGroupAddedEventImpl(String id, Long timestamp, TaskCandidateGroup taskCandidateGroup) {
        super(id, timestamp, taskCandidateGroup);
        setEntityId(taskCandidateGroup.getGroupId());
    }

    @Override
    public TaskCandidateGroupEvent.TaskCandidateGroupEvents getEventType() {
        return TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED;
    }
}
