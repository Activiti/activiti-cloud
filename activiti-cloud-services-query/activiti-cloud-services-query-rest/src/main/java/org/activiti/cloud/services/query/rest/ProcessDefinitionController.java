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
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessDefinitionResourceAssembler;
import org.activiti.cloud.services.security.ProcessDefinitionRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

@RestController
@ExposesResourceFor(ProcessDefinitionEntity.class)
@RequestMapping(
        value = "/v1/process-definitions",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class ProcessDefinitionController {

    private ProcessDefinitionRepository repository;

    private AlfrescoPagedResourcesAssembler<ProcessDefinitionEntity> pagedResourcesAssembler;

    private ProcessDefinitionResourceAssembler processDefinitionResourceAssembler;

    private ProcessDefinitionRestrictionService processDefinitionRestrictionService;

    public ProcessDefinitionController(ProcessDefinitionRepository repository,
                                       AlfrescoPagedResourcesAssembler<ProcessDefinitionEntity> pagedResourcesAssembler,
                                       ProcessDefinitionResourceAssembler processDefinitionResourceAssembler,
                                       ProcessDefinitionRestrictionService processDefinitionRestrictionService) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.processDefinitionResourceAssembler = processDefinitionResourceAssembler;
        this.processDefinitionRestrictionService = processDefinitionRestrictionService;
    }

    @GetMapping
    public PagedResources<Resource<CloudProcessDefinition>> findAll(@QuerydslPredicate(root = ProcessDefinitionEntity.class) Predicate predicate,
                                                                    Pageable pageable) {
        
        Predicate extendedPredicate = processDefinitionRestrictionService.restrictProcessDefinitionQuery(Optional.ofNullable(predicate)
                                                                                                                 .orElseGet(BooleanBuilder::new),
                                                                                                         SecurityPolicyAccess.READ);
        return pagedResourcesAssembler.toResource(pageable,
                                                  repository.findAll(extendedPredicate,
                                                                     pageable),
                                                  processDefinitionResourceAssembler);
    }
}
