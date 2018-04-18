package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.converter.TaskCandidateUserConverter;
import org.activiti.cloud.services.events.TaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.services.events.builders.ApplicationBuilderService;
import org.activiti.cloud.services.events.builders.ServiceBuilderService;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.task.IdentityLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.activiti.engine.delegate.event.ActivitiEventType.ENTITY_DELETED;

@Component
public class TaskCandidateUserRemovedEventConverter extends AbstractEventConverter {

    private final TaskCandidateUserConverter taskCandidateUserConverter;

    @Autowired
    public TaskCandidateUserRemovedEventConverter(TaskCandidateUserConverter identityLinkConverter,
                                                  ServiceBuilderService serviceBuilderService, ApplicationBuilderService applicationBuilderService) {
        super(applicationBuilderService,serviceBuilderService);
        this.taskCandidateUserConverter = identityLinkConverter;
    }

    @Override
    public ProcessEngineEvent from(ActivitiEvent event) {
        return new TaskCandidateUserRemovedEventImpl(buildService(),
                                                     buildApplication(),
                                                     event.getExecutionId(),
                                                     event.getProcessDefinitionId(),
                                                     event.getProcessInstanceId(),
                                                     taskCandidateUserConverter.from((IdentityLink) ((ActivitiEntityEvent) event).getEntity()));
    }

    @Override
    public String handledType() {
        return "TaskCandidateUser:" + ENTITY_DELETED.toString();
    }
}
