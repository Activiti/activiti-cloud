/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query.rest;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.cloud.services.query.resources.ProcessInstanceResource;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/" + ProcessInstanceRelProvider.COLLECTION_RESOURCE_REL, produces = MediaTypes.HAL_JSON_VALUE)
public class ProcessInstanceController {

    private final ProcessInstanceRepository processInstanceRepository;

    private ProcessInstanceResourceAssembler processInstanceResourceAssembler;

    private PagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler;

    private EntityFinder entityFinder;

    @Autowired
    public ProcessInstanceController(ProcessInstanceRepository processInstanceRepository,
                                     ProcessInstanceResourceAssembler processInstanceResourceAssembler,
                                     PagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler,
                                     EntityFinder entityFinder) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceResourceAssembler = processInstanceResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.entityFinder = entityFinder;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<ProcessInstanceResource> findAll(@QuerydslPredicate(root = ProcessInstance.class) Predicate predicate,
                                                           Pageable pageable) {
        return pagedResourcesAssembler.toResource(processInstanceRepository.findAll(predicate,
                                                                                    pageable),
                                                  processInstanceResourceAssembler);
    }

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    public ProcessInstanceResource findById(@PathVariable String processInstanceId) {
        return processInstanceResourceAssembler.toResource(entityFinder.findById(processInstanceRepository,
                                                                                 processInstanceId,
                                                                                 "Unable to find task for the given id:'" + processInstanceId + "'"));
    }
}
