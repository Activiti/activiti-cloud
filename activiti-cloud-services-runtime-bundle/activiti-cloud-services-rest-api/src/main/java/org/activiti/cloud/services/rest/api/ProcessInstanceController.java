package org.activiti.cloud.services.rest.api;

import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping(value = "/v1/process-instances", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessInstanceController {

    @RequestMapping(method = RequestMethod.GET)
    PagedResources<ProcessInstanceResource> getProcessInstances(Pageable pageable);

    @RequestMapping(method = RequestMethod.POST)
    ProcessInstanceResource startProcess(@RequestBody StartProcessPayload cmd);

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    ProcessInstanceResource getProcessInstanceById(@PathVariable String processInstanceId);

    @RequestMapping(value = "/{processInstanceId}/model",
            method = RequestMethod.GET,
            produces = "image/svg+xml")
    @ResponseBody
    String getProcessDiagram(@PathVariable String processInstanceId);

    @RequestMapping(value = "/signal")
    ResponseEntity<Void> sendSignal(@RequestBody SignalPayload signalPayload);

    @RequestMapping(value = "{processInstanceId}/suspend", method = RequestMethod.POST)
    ProcessInstanceResource suspend(@PathVariable String processInstanceId);

    @RequestMapping(value = "{processInstanceId}/resume", method = RequestMethod.POST)
    ProcessInstanceResource resume(@RequestBody String processInstanceId);

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.DELETE)
    ProcessInstanceResource deleteProcessInstance(@PathVariable String processInstanceId);
}
