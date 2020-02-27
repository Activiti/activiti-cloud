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

import java.util.Optional;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

@RestController
@RequestMapping(
        value = "${activiti.cloud.query.uriprefix}/admin/v1/process-instances",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class ProcessInstanceAdminController {

    private final ProcessInstanceRepository processInstanceRepository;

    private ProcessInstanceResourceAssembler processInstanceResourceAssembler;

    private AlfrescoPagedResourcesAssembler<ProcessInstanceEntity> pagedResourcesAssembler;
    
    private EntityFinder entityFinder;

    @Autowired
    public ProcessInstanceAdminController(ProcessInstanceRepository processInstanceRepository,
                                          ProcessInstanceResourceAssembler processInstanceResourceAssembler,
                                          AlfrescoPagedResourcesAssembler<ProcessInstanceEntity> pagedResourcesAssembler,
                                          EntityFinder entityFinder) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceResourceAssembler = processInstanceResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.entityFinder=entityFinder;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<Resource<CloudProcessInstance>> findAll(@QuerydslPredicate(root = ProcessInstanceEntity.class) Predicate predicate,
                                                                  Pageable pageable) {

        predicate = Optional.ofNullable(predicate)
                            .orElseGet(BooleanBuilder::new);
        
        return pagedResourcesAssembler.toResource(pageable,
                                                  processInstanceRepository.findAll(predicate,
                                                                                    pageable),
                                                  processInstanceResourceAssembler);
    }
    
    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    public Resource<CloudProcessInstance> findById(@PathVariable String processInstanceId) {

        ProcessInstanceEntity processInstanceEntity = entityFinder.findById(processInstanceRepository,
                                                                            processInstanceId,
                                                                            "Unable to find task for the given id:'" + processInstanceId + "'");
        return processInstanceResourceAssembler.toResource(processInstanceEntity);
    }



}
