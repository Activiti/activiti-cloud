package org.activiti.cloud.services.rest.assemblers;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionMetaControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

public class ProcessDefinitionMetaResourceAssembler implements ResourceAssembler<ProcessDefinitionMeta, Resource<ProcessDefinitionMeta>> {

    @Override
    public  Resource<ProcessDefinitionMeta> toResource(ProcessDefinitionMeta processDefinitionMeta) {

        Link metadata = linkTo(methodOn(ProcessDefinitionMetaControllerImpl.class).getProcessDefinitionMetadata(processDefinitionMeta.getId())).withRel("meta");
        Link selfRel = linkTo(methodOn(ProcessDefinitionControllerImpl.class).getProcessDefinition(processDefinitionMeta.getId())).withSelfRel();
        Link startProcessLink = linkTo(methodOn(ProcessInstanceControllerImpl.class).startProcess(null)).withRel("startProcess");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");

        return new Resource<>(processDefinitionMeta,
                              metadata,
                              selfRel,
                              startProcessLink,
                              homeLink);
    }
}
