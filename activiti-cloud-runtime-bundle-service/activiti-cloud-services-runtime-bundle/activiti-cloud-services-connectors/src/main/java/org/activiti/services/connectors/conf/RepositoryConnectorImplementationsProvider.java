package org.activiti.services.connectors.conf;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class RepositoryConnectorImplementationsProvider implements ConnectorImplementationsProvider {

    private final RepositoryService repositoryService;

    public RepositoryConnectorImplementationsProvider(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public List<String> getImplementations() {
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();

        return list.stream()
                   .map(ProcessDefinition::getId)
                   .map(repositoryService::getBpmnModel)
                   .flatMap(model -> model.getProcesses()
                                          .stream())
                   .flatMap(process -> process.getFlowElements()
                                              .stream())
                   .filter(ServiceTask.class::isInstance)
                   .map(ServiceTask.class::cast)
                   .filter(task -> !StringUtils.hasText(task.getImplementationType()))
                   .map(ServiceTask::getImplementation)
                   .distinct()
                   .collect(Collectors.toList());
    };
}
