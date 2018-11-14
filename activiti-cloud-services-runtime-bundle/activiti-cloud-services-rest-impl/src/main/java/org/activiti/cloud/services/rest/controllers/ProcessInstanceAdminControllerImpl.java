/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessInstanceAdminController;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceResourceAssembler;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceAdminControllerImpl implements ProcessInstanceAdminController {

    private final ProcessInstanceResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler;

    private final ProcessAdminRuntime processAdminRuntime;

    private final SpringPageConverter pageConverter;

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAppException(ActivitiForbiddenException ex) {
        return ex.getMessage();
    }


    @ExceptionHandler(ActivitiObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(ActivitiObjectNotFoundException ex) {
        return ex.getMessage();
    }

    public ProcessInstanceAdminControllerImpl(ProcessInstanceResourceAssembler resourceAssembler,
                                              AlfrescoPagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler,
                                              ProcessAdminRuntime processAdminRuntime,
                                              SpringPageConverter pageConverter) {
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.processAdminRuntime = processAdminRuntime;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedResources<ProcessInstanceResource> getAllProcessInstances(Pageable pageable) {
        Page<ProcessInstance> processInstancePage = processAdminRuntime.processInstances(pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable,
                                                  pageConverter.toSpringPage(pageable, processInstancePage),
                                                  resourceAssembler);
    }

    @Override
    public ProcessInstanceResource startProcess(@RequestBody StartProcessPayload startProcessPayload) {
        return resourceAssembler.toResource(processAdminRuntime.start(startProcessPayload));
    }


	@Override
	public ProcessInstanceResource suspend(@PathVariable String processInstanceId) {
		return resourceAssembler.toResource(processAdminRuntime.suspend(ProcessPayloadBuilder.suspend(processInstanceId)));
	}
	

}
