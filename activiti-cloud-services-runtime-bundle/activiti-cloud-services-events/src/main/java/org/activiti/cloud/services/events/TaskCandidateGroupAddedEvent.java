package org.activiti.cloud.services.events;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.TaskCandidateGroup;

public interface TaskCandidateGroupAddedEvent extends ProcessEngineEvent {

    TaskCandidateGroup getTaskCandidateGroup();
}
