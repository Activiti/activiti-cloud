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

package org.activiti.cloud.api.task.model.impl.events;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskUpdatedEvent;

public class CloudTaskUpdatedEventImpl extends CloudRuntimeEventImpl<Task, TaskRuntimeEvent.TaskEvents>
        implements CloudTaskUpdatedEvent {

    public CloudTaskUpdatedEventImpl() {
    }

    public CloudTaskUpdatedEventImpl(Task task) {
        super(task);
        setEntityId(task.getId());
    }

    public CloudTaskUpdatedEventImpl(String id,
                                     Long timestamp,
                                     Task task) {
        super(id,
              timestamp,
              task);
        setEntityId(task.getId());
    }

    @Override
    public TaskEvents getEventType() {
        return TaskEvents.TASK_UPDATED;
    }
}
