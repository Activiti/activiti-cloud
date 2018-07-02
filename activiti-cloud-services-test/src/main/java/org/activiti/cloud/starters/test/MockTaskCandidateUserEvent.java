package org.activiti.cloud.starters.test;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.TaskCandidateUser;

public class MockTaskCandidateUserEvent extends MockProcessEngineEvent {
    private TaskCandidateUser taskCandidateUser;

    public MockTaskCandidateUserEvent(Long timestamp, String eventType) {
        super(timestamp,
                eventType);
    }

    public static ProcessEngineEvent[] aTaskCandidateUserAddedEvent(long timestamp,
                                                         TaskCandidateUser taskCandidateUser,
                                                         String processInstanceId) {
        MockTaskCandidateUserEvent taskCandidateUserAddedEvent = new MockTaskCandidateUserEvent(timestamp,
                "TaskCandidateUserAddedEvent");
        taskCandidateUserAddedEvent.setTaskCandidateUser(taskCandidateUser);
        taskCandidateUserAddedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCandidateUserAddedEvent};
        return events;
    }

    public static ProcessEngineEvent[] aTaskCandidateUserRemovedEvent(long timestamp,
                                                                    TaskCandidateUser taskCandidateUser,
                                                                    String processInstanceId) {
        MockTaskCandidateUserEvent taskCandidateUserRemovedEvent = new MockTaskCandidateUserEvent(timestamp,
                "TaskCandidateUserRemovedEvent");
        taskCandidateUserRemovedEvent.setTaskCandidateUser(taskCandidateUser);
        taskCandidateUserRemovedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCandidateUserRemovedEvent};
        return events;
    }

    public TaskCandidateUser getTaskCandidateUser() {
        return taskCandidateUser;
    }

    public void setTaskCandidateUser(TaskCandidateUser taskCandidateUser) {
        this.taskCandidateUser = taskCandidateUser;
    }
}
