package org.activiti.cloud.services.rest.api;

import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.services.rest.api.resources.VariableInstanceResource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1/process-instances/{processInstanceId}/variables", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessInstanceVariableController {

    @RequestMapping(method = RequestMethod.GET)
    Resources<VariableInstanceResource> getVariables(@PathVariable String processInstanceId);

    @RequestMapping(value = "/local",
            method = RequestMethod.GET)
    Resources<VariableInstanceResource> getVariablesLocal(@PathVariable String processInstanceId);

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Void> setVariables(@PathVariable String processInstanceId,
                                      @RequestBody SetProcessVariablesPayload setProcessVariablesPayload);

    @RequestMapping(method = RequestMethod.DELETE)
    ResponseEntity<Void> removeVariables(@PathVariable String processInstanceId,
                                         @RequestBody RemoveProcessVariablesPayload removeProcessVariablesPayload);
}
