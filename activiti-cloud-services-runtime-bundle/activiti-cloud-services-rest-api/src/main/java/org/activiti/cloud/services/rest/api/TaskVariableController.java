package org.activiti.cloud.services.rest.api;

import org.activiti.cloud.services.rest.api.resources.VariableInstanceResource;
import org.activiti.runtime.api.model.payloads.SetTaskVariablesPayload;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1/tasks/{taskId}/variables",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface TaskVariableController {

    @RequestMapping(method = RequestMethod.GET)
    Resources<VariableInstanceResource> getVariables(@PathVariable String taskId);

    @RequestMapping(value = "/local",
            method = RequestMethod.GET)
    Resources<VariableInstanceResource> getVariablesLocal(@PathVariable String taskId);

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Void> setVariables(@PathVariable String taskId,
                                      @RequestBody SetTaskVariablesPayload setTaskVariablesPayload);

    @RequestMapping(value = "/local",
            method = RequestMethod.POST)
    ResponseEntity<Void> setVariablesLocal(@PathVariable String taskId,
                                           @RequestBody SetTaskVariablesPayload setTaskVariablesCmd);
}
