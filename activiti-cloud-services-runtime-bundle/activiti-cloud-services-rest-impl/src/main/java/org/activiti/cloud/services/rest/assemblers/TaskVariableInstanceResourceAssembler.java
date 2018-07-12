package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.rest.api.resources.VariableInstanceResource;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskVariableControllerImpl;
import org.activiti.runtime.api.model.CloudVariableInstance;
import org.activiti.runtime.api.model.VariableInstance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class TaskVariableInstanceResourceAssembler extends ResourceAssemblerSupport<VariableInstance,VariableInstanceResource> {

    private ToCloudVariableInstanceConverter converter;

    public TaskVariableInstanceResourceAssembler(ToCloudVariableInstanceConverter converter) {
        super(TaskVariableControllerImpl.class,
                VariableInstanceResource.class);
        this.converter = converter;
    }

    @Override
    public VariableInstanceResource toResource(VariableInstance taskVariable) {
        CloudVariableInstance cloudVariableInstance = converter.from(taskVariable);
        Link globalVariables = linkTo(methodOn(TaskVariableControllerImpl.class).getVariables(cloudVariableInstance.getTaskId())).withRel("globalVariables");
        Link localVariables = linkTo(methodOn(TaskVariableControllerImpl.class).getVariablesLocal(cloudVariableInstance.getTaskId())).withRel("localVariables");
        Link taskRel = linkTo(methodOn(TaskControllerImpl.class).getTaskById(cloudVariableInstance.getTaskId())).withRel("task");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new VariableInstanceResource(cloudVariableInstance, globalVariables, localVariables, taskRel, homeLink);
    }
}
