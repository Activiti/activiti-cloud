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

public abstract class CloudTaskEventImpl extends CloudRuntimeEventImpl<Task, TaskRuntimeEvent.TaskEvents> {

    public CloudTaskEventImpl() {
    }

    public CloudTaskEventImpl(Task task) {
        super(task);
        setFlattenInformation(task);
    }

    private void setFlattenInformation(Task task) {
        if (task != null) {
            setEntityId(task.getId());
            setProcessDefinitionId(task.getProcessDefinitionId());
            setProcessInstanceId(task.getProcessInstanceId());
            setProcessDefinitionVersion(task.getProcessDefinitionVersion());
        }
    }

    public CloudTaskEventImpl(String id,
                              Long timestamp,
                              Task task) {
        super(id,
              timestamp,
              task);
        setFlattenInformation(task);
    }

}
