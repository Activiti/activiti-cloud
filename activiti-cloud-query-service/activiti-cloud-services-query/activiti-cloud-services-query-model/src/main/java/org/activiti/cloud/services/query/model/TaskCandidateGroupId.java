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
package org.activiti.cloud.services.query.model;

import java.io.Serializable;

public class TaskCandidateGroupId implements Serializable {

    private static final long serialVersionUID = 1L;
    private String taskId;
    private String groupId;

    public TaskCandidateGroupId() {}

    public TaskCandidateGroupId(String taskId, String groupId) {
        this.taskId = taskId;
        this.groupId = groupId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getGroupId() {
        return groupId;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TaskCandidateGroupId other = (TaskCandidateGroupId) obj;
        if (taskId == null) {
            if (other.taskId != null) return false;
        } else if (!taskId.equals(other.taskId)) return false;
        if (groupId != other.groupId) return false;
        return true;
    }
}
