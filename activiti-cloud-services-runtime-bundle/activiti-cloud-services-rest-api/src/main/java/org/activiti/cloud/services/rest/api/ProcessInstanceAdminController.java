package org.activiti.cloud.services.rest.api;

import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/admin/v1/process-instances", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessInstanceAdminController {

    @RequestMapping(method = RequestMethod.GET)
    PagedResources<ProcessInstanceResource> getAllProcessInstances(Pageable pageable);

    @RequestMapping(method = RequestMethod.POST)
    ProcessInstanceResource startProcess(@RequestBody StartProcessPayload cmd);
    
    @RequestMapping(method = RequestMethod.POST,value = "{processInstanceId}/suspend")
    ProcessInstanceResource suspend(@PathVariable String processInstanceId);


    @RequestMapping(method = RequestMethod.POST,value = "{processInstanceId}/resume")
    ProcessInstanceResource resume(@PathVariable String processInstanceId);
    
    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.PUT)
    ProcessInstanceResource updateProcess(@PathVariable("processInstanceId") String processInstanceId,
                                    @RequestBody UpdateProcessPayload payload);
    
    @RequestMapping(value = "/{processInstanceId}/subprocesses", method = RequestMethod.GET)
    PagedResources<ProcessInstanceResource> subprocesses(@PathVariable("processInstanceId") String processInstanceId,
                                                         Pageable pageable);

}
