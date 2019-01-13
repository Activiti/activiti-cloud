package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.rest.api.resources.ConnectorDefinitionResource;
import org.activiti.cloud.services.rest.controllers.ConnectorDefinitionControllerImpl;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.core.common.model.connector.ConnectorDefinition;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class ConnectorDefinitionResourceAssembler extends ResourceAssemblerSupport<ConnectorDefinition, ConnectorDefinitionResource> {

    public ConnectorDefinitionResourceAssembler() {
        super(ConnectorDefinitionControllerImpl.class,
                ConnectorDefinitionResource.class);
    }

    @Override
    public ConnectorDefinitionResource toResource(ConnectorDefinition connectorDefinition) {

        Link selfRel = linkTo(methodOn(ConnectorDefinitionControllerImpl.class).getConnectorDefinition(connectorDefinition.getId())).withSelfRel();
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");

        return new ConnectorDefinitionResource(connectorDefinition,
                selfRel,
                homeLink);
    }
}
