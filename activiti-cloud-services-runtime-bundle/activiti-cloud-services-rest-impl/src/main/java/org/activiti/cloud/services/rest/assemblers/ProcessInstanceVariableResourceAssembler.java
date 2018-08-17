package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.api.resources.VariableInstanceResource;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class ProcessInstanceVariableResourceAssembler extends ResourceAssemblerSupport<VariableInstance, VariableInstanceResource> {

    private ToCloudVariableInstanceConverter converter;

    public ProcessInstanceVariableResourceAssembler(ToCloudVariableInstanceConverter converter) {
        super(ProcessInstanceVariableControllerImpl.class,
                VariableInstanceResource.class);
        this.converter = converter;
    }

    @Override
    public VariableInstanceResource toResource(VariableInstance variableInstance) {
        CloudVariableInstance cloudVariableInstance = converter.from(variableInstance);
        Link processVariables = linkTo(methodOn(ProcessInstanceVariableControllerImpl.class).getVariables(cloudVariableInstance.getProcessInstanceId())).withRel("processVariables");
        Link processInstance = linkTo(methodOn(ProcessInstanceControllerImpl.class).getProcessInstanceById(cloudVariableInstance.getProcessInstanceId())).withRel("processInstance");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new VariableInstanceResource(cloudVariableInstance, processVariables, processInstance, homeLink);
    }
}
