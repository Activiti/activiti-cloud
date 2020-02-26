package org.activiti.cloud.services.rest.assemblers;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.activiti.cloud.services.rest.controllers.ConnectorDefinitionControllerImpl;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.core.common.model.connector.ConnectorDefinition;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

public class ConnectorDefinitionResourceAssembler implements ResourceAssembler<ConnectorDefinition, Resource<ConnectorDefinition>> {

    @Override
    public Resource<ConnectorDefinition> toResource(ConnectorDefinition connectorDefinition) {

        Link selfRel = linkTo(methodOn(ConnectorDefinitionControllerImpl.class).getConnectorDefinition(connectorDefinition.getId())).withSelfRel();
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");

        return new Resource<>(connectorDefinition,
                selfRel,
                homeLink);
    }
}
