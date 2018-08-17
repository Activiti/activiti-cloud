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

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.rest.api.resources.ProcessDefinitionResource;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class ProcessDefinitionResourceAssembler extends ResourceAssemblerSupport<ProcessDefinition, ProcessDefinitionResource> {

    private ToCloudProcessDefinitionConverter converter;

    public ProcessDefinitionResourceAssembler(ToCloudProcessDefinitionConverter converter) {
        super(ProcessDefinitionControllerImpl.class,
              ProcessDefinitionResource.class);
        this.converter = converter;
    }

    @Override
    public ProcessDefinitionResource toResource(ProcessDefinition processDefinition) {
        CloudProcessDefinition cloudProcessDefinition = converter.from(processDefinition);
        Link selfRel = linkTo(methodOn(ProcessDefinitionControllerImpl.class).getProcessDefinition(cloudProcessDefinition.getId())).withSelfRel();
        Link startProcessLink = linkTo(methodOn(ProcessInstanceControllerImpl.class).startProcess(null)).withRel("startProcess");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new ProcessDefinitionResource(cloudProcessDefinition,
                                             selfRel,
                                             startProcessLink,
                                             homeLink);
    }
}
