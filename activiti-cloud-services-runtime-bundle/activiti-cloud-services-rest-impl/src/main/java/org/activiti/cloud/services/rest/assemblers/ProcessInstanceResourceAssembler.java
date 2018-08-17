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

package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class ProcessInstanceResourceAssembler extends ResourceAssemblerSupport<ProcessInstance, ProcessInstanceResource> {

    private ToCloudProcessInstanceConverter toCloudProcessInstanceConverter;

    public ProcessInstanceResourceAssembler(ToCloudProcessInstanceConverter toCloudProcessInstanceConverter) {
        super(ProcessInstanceControllerImpl.class,
              ProcessInstanceResource.class);
        this.toCloudProcessInstanceConverter = toCloudProcessInstanceConverter;
    }

    @Override
    public ProcessInstanceResource toResource(ProcessInstance processInstance) {
        CloudProcessInstance cloudProcessInstance = toCloudProcessInstanceConverter.from(processInstance);
        Link processInstancesRel = linkTo(methodOn(ProcessInstanceControllerImpl.class).getProcessInstances(null))
                .withRel("processInstances");
        Link selfLink = linkTo(methodOn(ProcessInstanceControllerImpl.class).getProcessInstanceById(cloudProcessInstance.getId())).withSelfRel();
        Link variablesLink = linkTo(methodOn(ProcessInstanceVariableControllerImpl.class).getVariables(cloudProcessInstance.getId())).withRel("variables");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new ProcessInstanceResource(cloudProcessInstance,
                                           selfLink,
                                           variablesLink,
                                           processInstancesRel,
                                           homeLink);
    }
}
