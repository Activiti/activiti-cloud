package org.activiti.cloud.services.rest.api;


import org.activiti.cloud.services.api.commands.RemoveProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.SetProcessVariablesCmd;
import org.activiti.cloud.services.rest.api.resources.ProcessVariableResource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.Map;

@RequestMapping(value = "/v1/process-instances/{processInstanceId}/variables", produces = MediaTypes.HAL_JSON_VALUE)
public interface ProcessInstanceVariableController {

    @RequestMapping(method = RequestMethod.GET)
    Resources<ProcessVariableResource> getVariables(@PathVariable String processInstanceId);

    @RequestMapping(value = "/local",
            method = RequestMethod.GET)
    Resources<ProcessVariableResource> getVariablesLocal(@PathVariable String processInstanceId);


    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Void> setVariables(@PathVariable String processInstanceId,
                                      @RequestBody SetProcessVariablesCmd setTaskVariablesCmd);

    @RequestMapping(method = RequestMethod.DELETE)
    ResponseEntity<Void> removeVariables(@PathVariable String processInstanceId,
                                         @RequestBody RemoveProcessVariablesCmd removeProcessVariablesCmd);
}
