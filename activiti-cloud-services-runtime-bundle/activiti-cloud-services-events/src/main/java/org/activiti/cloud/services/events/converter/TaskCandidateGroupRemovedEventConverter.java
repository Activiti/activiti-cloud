package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.converter.TaskCandidateGroupConverter;
import org.activiti.cloud.services.events.TaskCandidateGroupRemovedEventImpl;
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
public class TaskCandidateGroupRemovedEventConverter extends AbstractEventConverter  {

    private final TaskCandidateGroupConverter taskCandidateGroupConverter;

    @Autowired
    public TaskCandidateGroupRemovedEventConverter(TaskCandidateGroupConverter identityLinkConverter,
                                                   ServiceBuilderService serviceBuilderService,
                                                   ApplicationBuilderService applicationBuilderService){
        super(applicationBuilderService,serviceBuilderService);
        this.taskCandidateGroupConverter = identityLinkConverter;
    }

    @Override
    public ProcessEngineEvent from(ActivitiEvent event) {
        return new TaskCandidateGroupRemovedEventImpl(buildService(),
                buildApplication(),
                event.getExecutionId(),
                event.getProcessDefinitionId(),
                event.getProcessInstanceId(),
                taskCandidateGroupConverter.from((IdentityLink) ((ActivitiEntityEvent) event).getEntity()));
    }

    @Override
    public String handledType() {
        return "TaskCandidateGroup:" + ENTITY_DELETED.toString();
    }
}
