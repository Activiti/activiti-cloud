package org.activiti.cloud.services.rest.api;

import org.activiti.cloud.services.api.commands.SignalCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
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
    Resource<ProcessInstance> startProcess(@RequestBody StartProcessInstanceCmd cmd);

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    Resource<ProcessInstance> getProcessInstanceById(@PathVariable String processInstanceId);

    @RequestMapping(value = "/{processInstanceId}/model",
            method = RequestMethod.GET,
            produces = "image/svg+xml",
            consumes="image/svg+xml")
    @ResponseBody
    String getProcessDiagram(@PathVariable String processInstanceId);

    @RequestMapping(value = "/signal")
    ResponseEntity<Void> sendSignal(@RequestBody SignalCmd cmd);

    @RequestMapping(value = "{processInstanceId}/suspend")
    ResponseEntity<Void> suspend(@PathVariable String processInstanceId);

    @RequestMapping(value = "{processInstanceId}/activate")
    ResponseEntity<Void> activate(@PathVariable String processInstanceId);

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.DELETE)
    void deleteProcessInstance(@PathVariable String processInstanceId);

}
