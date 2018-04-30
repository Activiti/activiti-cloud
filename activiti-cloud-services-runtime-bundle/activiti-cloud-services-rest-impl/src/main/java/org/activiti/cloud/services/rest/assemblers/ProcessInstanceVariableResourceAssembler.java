package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.ProcessInstanceVariable;
import org.activiti.cloud.services.rest.api.resources.ProcessVariableResource;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class ProcessInstanceVariableResourceAssembler extends ResourceAssemblerSupport<ProcessInstanceVariable,ProcessVariableResource> {

    public ProcessInstanceVariableResourceAssembler() {
        super(ProcessInstanceVariableControllerImpl.class,
                ProcessVariableResource.class);
    }

    @Override
    public ProcessVariableResource toResource(ProcessInstanceVariable processInstanceVariable) {
        Link processVariables = linkTo(methodOn(ProcessInstanceVariableControllerImpl.class).getVariables(processInstanceVariable.getProcessInstanceId())).withRel("processVariables");
        Link processInstance = linkTo(methodOn(ProcessInstanceControllerImpl.class).getProcessInstanceById(processInstanceVariable.getProcessInstanceId())).withRel("processInstance");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new ProcessVariableResource(processInstanceVariable,processVariables,processInstance,homeLink);
    }
}
