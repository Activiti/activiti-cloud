package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.converter.TaskCandidateUserConverter;
import org.activiti.cloud.services.events.TaskCandidateUserAddedEventImpl;
import org.activiti.cloud.services.events.builders.ApplicationBuilderService;
import org.activiti.cloud.services.events.builders.ServiceBuilderService;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.task.IdentityLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.activiti.engine.delegate.event.ActivitiEventType.ENTITY_CREATED;

@Component
public class TaskCandidateUserAddedEventConverter extends AbstractEventConverter  {

    private final TaskCandidateUserConverter taskCandidateUserConverter;

    @Autowired
    public TaskCandidateUserAddedEventConverter(TaskCandidateUserConverter identityLinkConverter,
                                                ServiceBuilderService serviceBuilderService,
                                                ApplicationBuilderService applicationBuilderService){
        super(applicationBuilderService,serviceBuilderService);
        this.taskCandidateUserConverter = identityLinkConverter;
    }

    @Override
    public ProcessEngineEvent from(ActivitiEvent event) {
        return new TaskCandidateUserAddedEventImpl(buildService(),
                buildApplication(),
                event.getExecutionId(),
                event.getProcessDefinitionId(),
                event.getProcessInstanceId(),
                taskCandidateUserConverter.from((IdentityLink) ((ActivitiEntityEvent) event).getEntity()));
    }

    @Override
    public String handledType() {
        return "TaskCandidateUser:" + ENTITY_CREATED.toString();
    }
}
