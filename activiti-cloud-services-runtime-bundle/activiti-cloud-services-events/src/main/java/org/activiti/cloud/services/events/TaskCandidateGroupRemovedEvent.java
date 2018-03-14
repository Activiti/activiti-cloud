package org.activiti.cloud.services.events;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.TaskCandidateGroup;

public interface TaskCandidateGroupRemovedEvent extends ProcessEngineEvent {

    TaskCandidateGroup getTaskCandidateGroup();
}
