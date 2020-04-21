package org.activiti.cloud.services.rest.api;

import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1/process-instances/{processInstanceId}/variables", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessInstanceVariableController {

    @RequestMapping(method = RequestMethod.GET)
    CollectionModel<EntityModel<CloudVariableInstance>> getVariables(@PathVariable String processInstanceId);

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Void> setVariables(@PathVariable String processInstanceId,
                                      @RequestBody SetProcessVariablesPayload setProcessVariablesPayload);

}
