package org.activiti.cloud.services.rest.assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionMetaControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class ProcessDefinitionMetaRepresentationModelAssembler implements RepresentationModelAssembler<ProcessDefinitionMeta, EntityModel<ProcessDefinitionMeta>> {

    @Override
    public  EntityModel<ProcessDefinitionMeta> toModel(ProcessDefinitionMeta processDefinitionMeta) {

        Link metadata = linkTo(methodOn(ProcessDefinitionMetaControllerImpl.class).getProcessDefinitionMetadata(processDefinitionMeta.getId())).withRel("meta");
        Link selfRel = linkTo(methodOn(ProcessDefinitionControllerImpl.class).getProcessDefinition(processDefinitionMeta.getId())).withSelfRel();
        Link startProcessLink = linkTo(methodOn(ProcessInstanceControllerImpl.class).startProcess(null)).withRel("startProcess");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");

        return new EntityModel<>(processDefinitionMeta,
                              metadata,
                              selfRel,
                              startProcessLink,
                              homeLink);
    }
}
