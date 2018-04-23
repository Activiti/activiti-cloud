/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.api.model.converter;

import java.util.List;

import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.activiti.cloud.services.api.model.Task.TaskStatus;

import static org.activiti.cloud.services.api.model.Task.TaskStatus.CREATED;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.SUSPENDED;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.CANCELLED;

@Component
public class TaskConverter implements ModelConverter<Task, org.activiti.cloud.services.api.model.Task> {

    private final ListConverter listConverter;

    @Autowired
    public TaskConverter(ListConverter listConverter) {
        this.listConverter = listConverter;
    }

    @Override
    public org.activiti.cloud.services.api.model.Task from(Task source) {
        org.activiti.cloud.services.api.model.Task task = null;
        if (source != null) {
            task = new org.activiti.cloud.services.api.model.Task(source.getId(),
                                                                  source.getOwner(),
                                                                  source.getAssignee(),
                                                                  source.getName(),
                                                                  source.getDescription(),
                                                                  source.getCreateTime(),
                                                                  source.getClaimTime(),
                                                                  source.getDueDate(),
                                                                  source.getPriority(),
                                                                  source.getProcessDefinitionId(),
                                                                  source.getProcessInstanceId(),
                                                                  source.getParentTaskId(),
                                                                  calculateStatus(source));
        }
        return task;
    }

    private TaskStatus calculateStatus(Task source) {
        if (source instanceof TaskEntity &&
                (((TaskEntity) source).isDeleted() || ((TaskEntity) source).isCanceled())) {
            return CANCELLED;
        } else if (source.isSuspended()) {
            return SUSPENDED;
        } else if (source.getAssignee() != null && !source.getAssignee().isEmpty()) {
            return ASSIGNED;
        }
        return CREATED;
    }

    @Override
    public List<org.activiti.cloud.services.api.model.Task> from(List<Task> tasks) {
        return listConverter.from(tasks,
                                  this);
    }
}
