package org.activiti.cloud.starters.test;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.Task;

public class MockTaskEvent extends MockProcessEngineEvent {

    private Task task;

    public MockTaskEvent(Long timestamp, String eventType) {
        super(timestamp,
              eventType);
    }

    public static ProcessEngineEvent[] aTaskCreatedEvent(long timestamp,
                                                         Task task,
                                                         String processInstanceId) {
        MockTaskEvent taskCreatedEvent = new MockTaskEvent(timestamp,
                                                           "TaskCreatedEvent");
        taskCreatedEvent.setTask(task);
        taskCreatedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCreatedEvent};
        return events;
    }

    public static ProcessEngineEvent[] aTaskAssignedEvent(long timestamp,
                                                       Task task) {
        MockTaskEvent taskAssignedEvent = new MockTaskEvent(timestamp,
                                                           "TaskAssignedEvent");
        taskAssignedEvent.setTask(task);
        ProcessEngineEvent[] events = {taskAssignedEvent};
        return events;
    }

    public static ProcessEngineEvent[] aTaskCompletedEvent(long timestamp,
                                                       Task task) {
        MockTaskEvent taskCompletedEvent = new MockTaskEvent(timestamp,
                                                           "TaskCompletedEvent");
        taskCompletedEvent.setTask(task);
        ProcessEngineEvent[] events = {taskCompletedEvent};
        return events;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
