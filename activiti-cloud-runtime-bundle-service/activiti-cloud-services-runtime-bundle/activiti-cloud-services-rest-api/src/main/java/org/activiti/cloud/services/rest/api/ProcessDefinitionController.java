package org.activiti.cloud.services.rest.api;

import io.swagger.annotations.ApiOperation;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping(value = "/v1/process-definitions",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessDefinitionController {

    @RequestMapping(method = RequestMethod.GET)
    PagedModel<EntityModel<CloudProcessDefinition>> getProcessDefinitions(Pageable pageable);


    @RequestMapping(value = "/{id}",
            method = RequestMethod.GET)
    EntityModel<CloudProcessDefinition> getProcessDefinition(@PathVariable String id);

    @RequestMapping(value = "/{id}/model",
            method = RequestMethod.GET,
            produces = "application/xml")
    @ResponseBody
    @ApiOperation("getProcessModel")
    String getProcessModel(@PathVariable String id);

    @RequestMapping(value = "/{id}/model",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    @ApiOperation("getProcessModel")
    String getBpmnModel(@PathVariable String id);

    @RequestMapping(value = "/{id}/model",
            method = RequestMethod.GET,
            produces = "image/svg+xml")
    @ResponseBody
    @ApiOperation("getProcessModel")
    String getProcessDiagram(@PathVariable String id);
}
