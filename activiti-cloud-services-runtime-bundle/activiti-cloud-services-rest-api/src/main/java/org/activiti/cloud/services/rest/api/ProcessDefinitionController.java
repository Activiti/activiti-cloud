package org.activiti.cloud.services.rest.api;

import org.activiti.cloud.services.rest.api.resources.ProcessDefinitionResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping(value = "/v1/process-definitions",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessDefinitionController {

    @RequestMapping(method = RequestMethod.GET)
    PagedResources getProcessDefinitions(Pageable pageable);


    @RequestMapping(value = "/{id}",
            method = RequestMethod.GET)
    ProcessDefinitionResource getProcessDefinition(@PathVariable String id);

    @RequestMapping(value = "/{id}/model",
            method = RequestMethod.GET,
            produces = "application/xml")
    @ResponseBody
    String getProcessModel(@PathVariable String id);

    @RequestMapping(value = "/{id}/model",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    String getBpmnModel(@PathVariable String id);

    @RequestMapping(value = "/{id}/model",
            method = RequestMethod.GET,
            produces = "image/svg+xml")
    @ResponseBody
    String getProcessDiagram(@PathVariable String id);
}
