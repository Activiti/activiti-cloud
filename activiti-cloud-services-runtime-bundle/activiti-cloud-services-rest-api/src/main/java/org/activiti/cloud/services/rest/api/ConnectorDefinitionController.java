package org.activiti.cloud.services.rest.api;

import org.activiti.cloud.services.rest.api.resources.ConnectorDefinitionResource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1/connector-definitions",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ConnectorDefinitionController {

    @RequestMapping(method = RequestMethod.GET)
    Resources<ConnectorDefinitionResource> getConnectorDefinitions();


    @RequestMapping(value = "/{id}",
            method = RequestMethod.GET)
    ConnectorDefinitionResource getConnectorDefinition(@PathVariable String id);

}
