package org.activiti.cloud.starters.test;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.TaskCandidateGroup;

public class MockTaskCandidateGroupEvent extends MockProcessEngineEvent {
    private TaskCandidateGroup taskCandidateGroup;

    public MockTaskCandidateGroupEvent(Long timestamp, String eventType) {
        super(timestamp,
                eventType);
    }

    public static ProcessEngineEvent[] aTaskCandidateGroupAddedEvent(long timestamp,
                                                         TaskCandidateGroup taskCandidateGroup,
                                                         String processInstanceId) {
        MockTaskCandidateGroupEvent taskCandidateGroupAddedEvent = new MockTaskCandidateGroupEvent(timestamp,
                "TaskCandidateGroupAddedEvent");
        taskCandidateGroupAddedEvent.setTaskCandidateGroup(taskCandidateGroup);
        taskCandidateGroupAddedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCandidateGroupAddedEvent};
        return events;
    }

    public static ProcessEngineEvent[] aTaskCandidateGroupRemovedEvent(long timestamp,
                                                                    TaskCandidateGroup taskCandidateGroup,
                                                                    String processInstanceId) {
        MockTaskCandidateGroupEvent taskCandidateGroupRemovedEvent = new MockTaskCandidateGroupEvent(timestamp,
                "TaskCandidateGroupRemovedEvent");
        taskCandidateGroupRemovedEvent.setTaskCandidateGroup(taskCandidateGroup);
        taskCandidateGroupRemovedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCandidateGroupRemovedEvent};
        return events;
    }

    public TaskCandidateGroup getTaskCandidateGroup() {
        return taskCandidateGroup;
    }

    public void setTaskCandidateGroup(TaskCandidateGroup taskCandidateGroup) {
        this.taskCandidateGroup = taskCandidateGroup;
    }
}
