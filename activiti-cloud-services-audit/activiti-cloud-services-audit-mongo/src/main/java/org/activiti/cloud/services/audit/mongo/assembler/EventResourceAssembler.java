package org.activiti.cloud.services.audit.mongo.assembler;

import org.activiti.cloud.services.audit.mongo.resources.EventResource;
import org.activiti.cloud.services.audit.mongo.ProcessEngineEventsController;
import org.activiti.cloud.services.audit.mongo.events.ProcessEngineEventDocument;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class EventResourceAssembler implements ResourceAssembler<ProcessEngineEventDocument, EventResource> {

    @Override
    public EventResource toResource(ProcessEngineEventDocument entity) {
        Link selfRel = linkTo(methodOn(ProcessEngineEventsController.class).findById(entity.getId())).withSelfRel();
        return new EventResource(entity, selfRel);
    }

}