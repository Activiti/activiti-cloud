package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.rest.api.resources.VariableInstanceResource;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableControllerImpl;
import org.activiti.runtime.api.model.VariableInstance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class ProcessInstanceVariableResourceAssembler extends ResourceAssemblerSupport<VariableInstance, VariableInstanceResource> {

    public ProcessInstanceVariableResourceAssembler() {
        super(ProcessInstanceVariableControllerImpl.class,
                VariableInstanceResource.class);
    }

    @Override
    public VariableInstanceResource toResource(VariableInstance variableInstance) {
        Link processVariables = linkTo(methodOn(ProcessInstanceVariableControllerImpl.class).getVariables(variableInstance.getProcessInstanceId())).withRel("processVariables");
        Link processInstance = linkTo(methodOn(ProcessInstanceControllerImpl.class).getProcessInstanceById(variableInstance.getProcessInstanceId())).withRel("processInstance");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new VariableInstanceResource(variableInstance,processVariables,processInstance,homeLink);
    }
}
