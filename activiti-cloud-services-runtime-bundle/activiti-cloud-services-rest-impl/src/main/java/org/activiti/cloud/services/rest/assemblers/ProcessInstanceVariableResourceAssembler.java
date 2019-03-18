package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class ProcessInstanceVariableResourceAssembler implements ResourceAssembler<VariableInstance, Resource<CloudVariableInstance>> {

    private ToCloudVariableInstanceConverter converter;

    public ProcessInstanceVariableResourceAssembler(ToCloudVariableInstanceConverter converter) {
        this.converter = converter;
    }

    @Override
    public Resource<CloudVariableInstance> toResource(VariableInstance variableInstance) {
        CloudVariableInstance cloudVariableInstance = converter.from(variableInstance);
        Link processVariables = linkTo(methodOn(ProcessInstanceVariableControllerImpl.class).getVariables(cloudVariableInstance.getProcessInstanceId())).withRel("processVariables");
        Link processInstance = linkTo(methodOn(ProcessInstanceControllerImpl.class).getProcessInstanceById(cloudVariableInstance.getProcessInstanceId())).withRel("processInstance");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new Resource<>(cloudVariableInstance,
                              processVariables,
                              processInstance,
                              homeLink);
    }
}
