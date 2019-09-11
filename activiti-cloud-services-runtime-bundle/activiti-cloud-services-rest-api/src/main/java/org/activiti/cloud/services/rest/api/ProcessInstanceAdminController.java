package org.activiti.cloud.services.rest.api;

import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
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

@RequestMapping(value = "/admin/v1/process-instances", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessInstanceAdminController {

    @RequestMapping(method = RequestMethod.GET)
    PagedResources<Resource<CloudProcessInstance>> getProcessInstances(Pageable pageable);

    @RequestMapping(method = RequestMethod.POST)
    Resource<CloudProcessInstance> startProcess(@RequestBody StartProcessPayload cmd);
    
    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    Resource<CloudProcessInstance> getProcessInstanceById(@PathVariable String processInstanceId);
    
    @RequestMapping(method = RequestMethod.POST,value = "{processInstanceId}/suspend")
    Resource<CloudProcessInstance> suspend(@PathVariable String processInstanceId);


    @RequestMapping(method = RequestMethod.POST,value = "{processInstanceId}/resume")
    Resource<CloudProcessInstance> resume(@PathVariable String processInstanceId);
    
    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.DELETE)
    Resource<CloudProcessInstance> deleteProcessInstance(@PathVariable String processInstanceId);
    
    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.PUT)
    Resource<CloudProcessInstance> updateProcess(@PathVariable("processInstanceId") String processInstanceId,
                                    @RequestBody UpdateProcessPayload payload);
    
    @RequestMapping(value = "/{processInstanceId}/subprocesses", method = RequestMethod.GET)
    PagedResources<Resource<CloudProcessInstance>> subprocesses(@PathVariable("processInstanceId") String processInstanceId,
                                                         Pageable pageable);
    
    @RequestMapping(value = "/message", method = RequestMethod.POST) 
    Resource<CloudProcessInstance> start(@RequestBody StartMessagePayload startMessagePayload);

    @RequestMapping(value = "/message", method = RequestMethod.PUT) 
    ResponseEntity<Void> receive(@RequestBody ReceiveMessagePayload receiveMessagePayload);
    
    
}
