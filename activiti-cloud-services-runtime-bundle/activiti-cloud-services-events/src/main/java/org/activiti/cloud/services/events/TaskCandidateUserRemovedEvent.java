package org.activiti.cloud.services.events;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.TaskCandidateUser;

public interface TaskCandidateUserRemovedEvent extends ProcessEngineEvent {

    TaskCandidateUser getTaskCandidateUser();
}
