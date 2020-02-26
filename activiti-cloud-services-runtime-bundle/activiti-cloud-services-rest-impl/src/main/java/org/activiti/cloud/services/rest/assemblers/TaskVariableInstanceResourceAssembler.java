package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskVariableControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class TaskVariableInstanceResourceAssembler implements ResourceAssembler<VariableInstance,Resource<CloudVariableInstance>> {

    private ToCloudVariableInstanceConverter converter;

    public TaskVariableInstanceResourceAssembler(ToCloudVariableInstanceConverter converter) {
        this.converter = converter;
    }

    @Override
    public Resource<CloudVariableInstance> toResource(VariableInstance taskVariable) {
        CloudVariableInstance cloudVariableInstance = converter.from(taskVariable);
        Link globalVariables = linkTo(methodOn(TaskVariableControllerImpl.class).getVariables(cloudVariableInstance.getTaskId())).withRel("variables");
        Link taskRel = linkTo(methodOn(TaskControllerImpl.class).getTaskById(cloudVariableInstance.getTaskId())).withRel("task");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new Resource<>(cloudVariableInstance,
                              globalVariables,
                              taskRel,
                              homeLink);
    }
}
