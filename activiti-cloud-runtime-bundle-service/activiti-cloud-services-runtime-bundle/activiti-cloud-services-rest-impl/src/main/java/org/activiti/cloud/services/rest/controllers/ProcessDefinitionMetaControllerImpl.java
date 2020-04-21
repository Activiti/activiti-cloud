package org.activiti.cloud.services.rest.controllers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.activiti.cloud.services.api.model.ProcessDefinitionServiceTask;
import org.activiti.cloud.services.api.model.ProcessDefinitionUserTask;
import org.activiti.cloud.services.api.model.ProcessDefinitionVariable;
import org.activiti.cloud.services.rest.api.ProcessDefinitionMetaController;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionMetaRepresentationModelAssembler;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessDefinitionMetaControllerImpl implements ProcessDefinitionMetaController {

    private final RepositoryService repositoryService;
    private final ProcessDefinitionMetaRepresentationModelAssembler representationModelAssembler;

    @Autowired
    public ProcessDefinitionMetaControllerImpl(RepositoryService repositoryService,
                                               ProcessDefinitionMetaRepresentationModelAssembler representationModelAssembler) {
        this.repositoryService = repositoryService;
        this.representationModelAssembler = representationModelAssembler;
    }

    @Override
    public EntityModel<ProcessDefinitionMeta> getProcessDefinitionMetadata(@PathVariable String id) {
        org.activiti.engine.repository.ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(id)
                .singleResult();
        if (processDefinition == null) {
            throw new ActivitiObjectNotFoundException("Unable to find process definition for the given id:'" + id + "'");
        }

        List<Process> processes = repositoryService.getBpmnModel(id).getProcesses();
        Set<ProcessDefinitionVariable> variables = new HashSet<>();
        Set<String> users = new HashSet<>();
        Set<String> groups = new HashSet<>();
        Set<ProcessDefinitionUserTask> userTasks = new HashSet<>();
        Set<ProcessDefinitionServiceTask> serviceTasks = new HashSet<>();

        for (Process process : processes) {
            variables.addAll(getVariables(process));
            List<FlowElement> flowElementList = (List<FlowElement>) process.getFlowElements();
            for (FlowElement flowElement : flowElementList) {
                if (flowElement.getClass().equals(UserTask.class)) {
                    UserTask userTask = (UserTask) flowElement;
                    ProcessDefinitionUserTask task = new ProcessDefinitionUserTask(userTask.getName(),
                                                                                   userTask.getDocumentation());
                    userTasks.add(task);
                    users.addAll(userTask.getCandidateUsers());
                    groups.addAll(userTask.getCandidateGroups());
                }
                if (flowElement.getClass().equals(ServiceTask.class)) {
                    ServiceTask serviceTask = (ServiceTask) flowElement;
                    ProcessDefinitionServiceTask task = new ProcessDefinitionServiceTask(serviceTask.getName(),
                                                                                         serviceTask.getImplementation());
                    serviceTasks.add(task);
                }
            }
        }

        return representationModelAssembler.toModel(new ProcessDefinitionMeta(processDefinition.getId(),
                                                                      processDefinition.getName(),
                                                                      processDefinition.getDescription(),
                                                                      processDefinition.getVersion(),
                                                                      users,
                                                                      groups,
                                                                      variables,
                                                                      userTasks,
                                                                      serviceTasks));
    }

    private List<ProcessDefinitionVariable> getVariables(Process process) {
        List<ProcessDefinitionVariable> variables = new ArrayList<>();
        if (!process.getExtensionElements().isEmpty()) {
            Iterator<List<ExtensionElement>> it = process.getExtensionElements().values().iterator();
            while (it.hasNext()) {
                List<ExtensionElement> extensionElementList = it.next();
                Iterator<ExtensionElement> it2 = extensionElementList.iterator();
                while (it2.hasNext()) {
                    ExtensionElement ee = it2.next();
                    String name = ee.getAttributeValue(ee.getNamespace(),
                                                       "variableName");
                    String type = ee.getAttributeValue(ee.getNamespace(),
                                                       "variableType");
                    ProcessDefinitionVariable variable = new ProcessDefinitionVariable(name,
                                                                                       type);
                    variables.add(variable);
                }
            }
        }
        return variables;
    }
}
