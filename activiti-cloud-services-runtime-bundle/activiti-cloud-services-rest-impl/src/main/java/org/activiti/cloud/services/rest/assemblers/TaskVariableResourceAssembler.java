package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.TaskVariable;
import org.activiti.cloud.services.rest.api.resources.TaskVariableResource;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskVariableControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class TaskVariableResourceAssembler extends ResourceAssemblerSupport<TaskVariable,TaskVariableResource> {

    public TaskVariableResourceAssembler() {
        super(TaskVariableControllerImpl.class,
                TaskVariableResource.class);
    }

    @Override
    public TaskVariableResource toResource(TaskVariable taskVariable) {
        Link globalVariables = linkTo(methodOn(TaskVariableControllerImpl.class).getVariables(taskVariable.getTaskId())).withRel("globalVariables");
        Link localVariables = linkTo(methodOn(TaskVariableControllerImpl.class).getVariablesLocal(taskVariable.getTaskId())).withRel("localVariables");
        Link taskRel = linkTo(methodOn(TaskControllerImpl.class).getTaskById(taskVariable.getTaskId())).withRel("task");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new TaskVariableResource(taskVariable,globalVariables,localVariables,taskRel,homeLink);
    }
}
