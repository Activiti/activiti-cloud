package org.activiti.cloud.services.rest.api;

import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1/process-definitions/{id}/meta",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessDefinitionMetaController {

    @RequestMapping(method = RequestMethod.GET)
    EntityModel<ProcessDefinitionMeta> getProcessDefinitionMetadata(@PathVariable String id);
}
