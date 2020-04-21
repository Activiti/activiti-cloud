package org.activiti.cloud.services.rest.assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.activiti.cloud.services.rest.controllers.ConnectorDefinitionControllerImpl;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.core.common.model.connector.ConnectorDefinition;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class ConnectorDefinitionRepresentationModelAssembler implements RepresentationModelAssembler<ConnectorDefinition, EntityModel<ConnectorDefinition>> {

    @Override
    public EntityModel<ConnectorDefinition> toModel(ConnectorDefinition connectorDefinition) {

        Link selfRel = linkTo(methodOn(ConnectorDefinitionControllerImpl.class).getConnectorDefinition(connectorDefinition.getId())).withSelfRel();
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");

        return new EntityModel<>(connectorDefinition,
                selfRel,
                homeLink);
    }
}
