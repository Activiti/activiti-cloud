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
package org.activiti.cloud.starters.test.builder;

import java.util.Date;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.api.task.model.impl.QueryCloudTaskImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCancelledEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;
import org.activiti.cloud.starters.test.EventsAggregator;

public class TaskEventContainedBuilder {

    private EventsAggregator eventsAggregator;

    public TaskEventContainedBuilder(EventsAggregator eventsAggregator) {
        this.eventsAggregator = eventsAggregator;
    }

    public Task aCreatedTask(String taskName, ProcessInstance processInstance) {
        Task task = buildTask(taskName, Task.TaskStatus.CREATED, processInstance);
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task));
        return task;
    }

    public Task aCreatedStandaloneTaskWithParent(String taskName) {
        Task task = buildTask(taskName, Task.TaskStatus.CREATED, null);
        ((TaskImpl) task).setParentTaskId(UUID.randomUUID().toString());
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task));
        return task;
    }

    public Task aCreatedStandaloneAssignedTaskWithParent(String taskName, String username) {
        Task task = buildTask(taskName, Task.TaskStatus.CREATED, null);
        ((TaskImpl) task).setParentTaskId(UUID.randomUUID().toString());
        ((TaskImpl) task).setAssignee(username);
        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task), new CloudTaskAssignedEventImpl(task));
        return task;
    }

    public Task anAssignedTask(String taskName, String username, ProcessInstance processInstance) {
        TaskImpl task = buildTask(taskName, Task.TaskStatus.ASSIGNED, processInstance);
        task.setAssignee(username);

        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task), new CloudTaskAssignedEventImpl(task));
        return task;
    }

    public QueryCloudTask anAssignedQueryCloudTask(String taskName, String username, ProcessInstance processInstance) {
        QueryCloudTaskImpl task = buildQueryCloudTask(taskName, Task.TaskStatus.ASSIGNED, processInstance);
        task.setAssignee(username);

        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task), new CloudTaskAssignedEventImpl(task));
        return task;
    }

    public Task anAssignedTaskWithDueDate(
        String taskName,
        String username,
        ProcessInstance processInstance,
        Date dueDate
    ) {
        TaskImpl task = buildTask(taskName, Task.TaskStatus.ASSIGNED, processInstance);
        task.setAssignee(username);
        task.setDueDate(dueDate);

        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task), new CloudTaskAssignedEventImpl(task));
        return task;
    }

    public Task anAssignedTaskWithParent(String taskName, String username, ProcessInstance processInstance) {
        TaskImpl task = buildTask(taskName, Task.TaskStatus.ASSIGNED, processInstance);
        task.setAssignee(username);
        task.setParentTaskId(UUID.randomUUID().toString());

        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task), new CloudTaskAssignedEventImpl(task));
        return task;
    }

    public Task aCompletedTask(String taskName, ProcessInstance processInstance) {
        Task task = buildTask(taskName, Task.TaskStatus.COMPLETED, processInstance);
        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskAssignedEventImpl(task),
            new CloudTaskCompletedEventImpl(UUID.randomUUID().toString(), new Date().getTime(), task)
        );
        return task;
    }

    public Task aCancelledTask(String taskName, ProcessInstance processInstance) {
        Task task = buildTask(taskName, Task.TaskStatus.CANCELLED, processInstance);
        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskAssignedEventImpl(task),
            new CloudTaskCancelledEventImpl(UUID.randomUUID().toString(), new Date().getTime(), task)
        );
        return task;
    }

    public Task aReleasedTask(String taskName) {
        Task task = buildTask(taskName, Task.TaskStatus.ASSIGNED, null);
        Task releasedTask = task;
        ((TaskImpl) releasedTask).setStatus(Task.TaskStatus.CREATED);
        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskAssignedEventImpl(task),
            new CloudTaskUpdatedEventImpl(releasedTask)
        );
        return task;
    }

    public Task aCompletedTaskWithCreationDateAndCompletionDate(
        String taskName,
        ProcessInstance processInstance,
        Date createdDate,
        Date completedDate
    ) {
        Task task = buildTask(taskName, Task.TaskStatus.COMPLETED, processInstance);

        ((TaskImpl) task).setCreatedDate(createdDate);

        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(
                "task-created-event-id" + UUID.randomUUID().toString(),
                createdDate.getTime(),
                task
            ),
            new CloudTaskAssignedEventImpl(task),
            new CloudTaskCompletedEventImpl(
                "task-completed-event-id" + UUID.randomUUID().toString(),
                completedDate.getTime(),
                task
            )
        );
        return task;
    }

    public Task aTaskWithUserCandidate(String taskName, String username, ProcessInstance processInstance) {
        TaskImpl task = buildTask(taskName, Task.TaskStatus.CREATED, processInstance);
        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskCandidateUserAddedEventImpl(new TaskCandidateUserImpl(username, task.getId()))
        );
        return task;
    }

    public QueryCloudTask aQueryCloudTaskWithUserCandidate(
        String taskName,
        String username,
        ProcessInstance processInstance
    ) {
        QueryCloudTaskImpl task = buildQueryCloudTask(taskName, Task.TaskStatus.CREATED, processInstance);
        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskCandidateUserAddedEventImpl(new TaskCandidateUserImpl(username, task.getId()))
        );
        return task;
    }

    public Task aTaskWithGroupCandidate(String taskName, String groupId, ProcessInstance processInstance) {
        TaskImpl task = buildTask(taskName, Task.TaskStatus.CREATED, processInstance);
        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskCandidateGroupAddedEventImpl(new TaskCandidateGroupImpl(groupId, task.getId()))
        );
        return task;
    }

    public QueryCloudTask aQueryCloudTaskWithGroupCandidate(
        String taskName,
        String groupId,
        ProcessInstance processInstance
    ) {
        QueryCloudTaskImpl task = buildQueryCloudTask(taskName, Task.TaskStatus.CREATED, processInstance);
        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskCandidateGroupAddedEventImpl(new TaskCandidateGroupImpl(groupId, task.getId()))
        );
        return task;
    }

    public Task aTaskWithTwoGroupCandidates(
        String taskName,
        String groupIdOne,
        String groupIdTwo,
        ProcessInstance processInstance,
        String userName
    ) {
        TaskImpl task = buildTask(taskName, Task.TaskStatus.CREATED, processInstance);
        task.setAssignee(userName);
        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskCandidateGroupAddedEventImpl(new TaskCandidateGroupImpl(groupIdOne, task.getId())),
            new CloudTaskCandidateGroupAddedEventImpl(new TaskCandidateGroupImpl(groupIdTwo, task.getId()))
        );
        return task;
    }

    public Task aTaskWithPriority(String taskName, int priority, ProcessInstance processInstance) {
        Task task = buildTask(taskName, Task.TaskStatus.CREATED, processInstance);

        ((TaskImpl) task).setPriority(priority);

        eventsAggregator.addEvents(new CloudTaskCreatedEventImpl(task), new CloudTaskCompletedEventImpl(task));
        return task;
    }

    public static TaskImpl buildTask(String taskName, Task.TaskStatus status, ProcessInstance processInstance) {
        TaskImpl task = new TaskImpl(UUID.randomUUID().toString(), taskName, status);
        task.setCreatedDate(new Date());
        if (processInstance != null) {
            task.setProcessInstanceId(processInstance.getId());
        }
        return task;
    }

    public static QueryCloudTaskImpl buildQueryCloudTask(
        String taskName,
        Task.TaskStatus status,
        ProcessInstance processInstance
    ) {
        QueryCloudTaskImpl task = new QueryCloudTaskImpl();
        task.setId(UUID.randomUUID().toString());
        task.setName(taskName);
        task.setStatus(status);
        task.setCreatedDate(new Date());
        if (processInstance != null) {
            task.setProcessInstanceId(processInstance.getId());
        }
        return task;
    }

    public Task aCompletedTaskWithCompletedBy(String taskName, ProcessInstance processInstance, String completedBy) {
        TaskImpl task = buildTask(taskName, Task.TaskStatus.COMPLETED, processInstance);
        task.setCompletedBy(completedBy);
        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskAssignedEventImpl(task),
            new CloudTaskCompletedEventImpl(UUID.randomUUID().toString(), new Date().getTime(), task)
        );
        return task;
    }

    public Task aCompletedTaskWithCompletionDate(String taskName, ProcessInstance processInstance, Date completedDate) {
        Task task = buildTask(taskName, Task.TaskStatus.COMPLETED, processInstance);

        ((TaskImpl) task).setCompletedDate(completedDate);

        eventsAggregator.addEvents(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskAssignedEventImpl(task),
            new CloudTaskCompletedEventImpl(
                "task-completed-event-id" + UUID.randomUUID().toString(),
                completedDate.getTime(),
                task
            )
        );
        return task;
    }
}
