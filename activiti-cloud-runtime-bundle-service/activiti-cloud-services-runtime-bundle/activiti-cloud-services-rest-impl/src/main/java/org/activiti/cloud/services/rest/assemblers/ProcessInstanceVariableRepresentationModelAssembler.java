package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class ProcessInstanceVariableRepresentationModelAssembler implements RepresentationModelAssembler<VariableInstance, EntityModel<CloudVariableInstance>> {

    private ToCloudVariableInstanceConverter converter;

    public ProcessInstanceVariableRepresentationModelAssembler(ToCloudVariableInstanceConverter converter) {
        this.converter = converter;
    }

    @Override
    public EntityModel<CloudVariableInstance> toModel(VariableInstance variableInstance) {
        CloudVariableInstance cloudVariableInstance = converter.from(variableInstance);
        Link processVariables = linkTo(methodOn(ProcessInstanceVariableControllerImpl.class).getVariables(cloudVariableInstance.getProcessInstanceId())).withRel("processVariables");
        Link processInstance = linkTo(methodOn(ProcessInstanceControllerImpl.class).getProcessInstanceById(cloudVariableInstance.getProcessInstanceId())).withRel("processInstance");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new EntityModel<>(cloudVariableInstance,
                              processVariables,
                              processInstance,
                              homeLink);
    }
}
