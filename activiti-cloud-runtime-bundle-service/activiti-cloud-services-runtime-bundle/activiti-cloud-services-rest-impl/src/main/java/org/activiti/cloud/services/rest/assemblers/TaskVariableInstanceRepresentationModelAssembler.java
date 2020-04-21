package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskVariableControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class TaskVariableInstanceRepresentationModelAssembler implements RepresentationModelAssembler<VariableInstance,EntityModel<CloudVariableInstance>> {

    private ToCloudVariableInstanceConverter converter;

    public TaskVariableInstanceRepresentationModelAssembler(ToCloudVariableInstanceConverter converter) {
        this.converter = converter;
    }

    @Override
    public EntityModel<CloudVariableInstance> toModel(VariableInstance taskVariable) {
        CloudVariableInstance cloudVariableInstance = converter.from(taskVariable);
        Link globalVariables = linkTo(methodOn(TaskVariableControllerImpl.class).getVariables(cloudVariableInstance.getTaskId())).withRel("variables");
        Link taskRel = linkTo(methodOn(TaskControllerImpl.class).getTaskById(cloudVariableInstance.getTaskId())).withRel("task");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new EntityModel<>(cloudVariableInstance,
                              globalVariables,
                              taskRel,
                              homeLink);
    }
}
