package org.activiti.cloud.services.rest.api;


import org.activiti.cloud.services.rest.api.resources.ProcessVariableResource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1/process-instances/{processInstanceId}/variables", produces = MediaTypes.HAL_JSON_VALUE)
public interface ProcessInstanceVariableController {

    @RequestMapping(value = "/",
            method = RequestMethod.GET)
    Resources<ProcessVariableResource> getVariables(@PathVariable String processInstanceId);

    @RequestMapping(value = "/local",
            method = RequestMethod.GET)
    Resources<ProcessVariableResource> getVariablesLocal(@PathVariable String processInstanceId);
}
