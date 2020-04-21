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

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceRepresentationModelAssembler;
import org.activiti.cloud.services.security.ProcessInstanceRestrictionService;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;

@RestController
@RequestMapping(
        value = "/v1/process-instances",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class ProcessInstanceController {

    private final ProcessInstanceRepository processInstanceRepository;

    private ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler;

    private AlfrescoPagedModelAssembler<ProcessInstanceEntity> pagedCollectionModelAssembler;

    private SecurityPoliciesManager securityPoliciesApplicationService;

    private ProcessInstanceRestrictionService processInstanceRestrictionService;

    private SecurityManager securityManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceController.class);

    private EntityFinder entityFinder;

    @Autowired
    public ProcessInstanceController(ProcessInstanceRepository processInstanceRepository,
                                     ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler,
                                     AlfrescoPagedModelAssembler<ProcessInstanceEntity> pagedCollectionModelAssembler,
                                     ProcessInstanceRestrictionService processInstanceRestrictionService,
                                     EntityFinder entityFinder,
                                     SecurityPoliciesManager securityPoliciesApplicationService,
                                     SecurityManager securityManager) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceRepresentationModelAssembler = processInstanceRepresentationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.processInstanceRestrictionService = processInstanceRestrictionService;
        this.entityFinder = entityFinder;
        this.securityPoliciesApplicationService = securityPoliciesApplicationService;
        this.securityManager = securityManager;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudProcessInstance>> findAll(@QuerydslPredicate(root = ProcessInstanceEntity.class) Predicate predicate,
                                                                  Pageable pageable) {

        predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(Optional.ofNullable(predicate)
                                                                                           .orElseGet(BooleanBuilder::new),
                                                                                    SecurityPolicyAccess.READ);

        return pagedCollectionModelAssembler.toModel(pageable,
                                                  processInstanceRepository.findAll(predicate,
                                                                                    pageable),
                                                  processInstanceRepresentationModelAssembler);
    }

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    public EntityModel<CloudProcessInstance> findById(@PathVariable String processInstanceId) {

        ProcessInstanceEntity processInstanceEntity = entityFinder.findById(processInstanceRepository,
                                                                            processInstanceId,
                                                                            "Unable to find process instance for the given id:'" + processInstanceId + "'");

        if (!securityPoliciesApplicationService.canRead(processInstanceEntity.getProcessDefinitionKey(),
                                                        processInstanceEntity.getServiceName())) {
            LOGGER.debug("User " + securityManager.getAuthenticatedUserId() + " not permitted to access definition " + processInstanceEntity.getProcessDefinitionKey());
            throw new ActivitiForbiddenException("Operation not permitted for " + processInstanceEntity.getProcessDefinitionKey());
        }

        return processInstanceRepresentationModelAssembler.toModel(processInstanceEntity);
    }


    @RequestMapping(value = "/{processInstanceId}/subprocesses", method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudProcessInstance>> subprocesses(@PathVariable String processInstanceId,
                                                                @QuerydslPredicate(root = ProcessInstanceEntity.class) Predicate predicate,
                                                                Pageable pageable) {

        predicate = Optional.ofNullable(predicate).orElseGet(BooleanBuilder::new);

        ProcessInstanceEntity processInstanceEntity = entityFinder.findById(processInstanceRepository,
                                                                            processInstanceId,
                                                                            "Unable to find process for the given id:'" + processInstanceId + "'");

        if (!securityPoliciesApplicationService.canRead(processInstanceEntity.getProcessDefinitionKey(),
                                                        processInstanceEntity.getServiceName())) {
            LOGGER.debug("User " + securityManager.getAuthenticatedUserId() + " not permitted to access definition " + processInstanceEntity.getProcessDefinitionKey());
            throw new ActivitiForbiddenException("Operation not permitted for " + processInstanceEntity.getProcessDefinitionKey());
        }

        QProcessInstanceEntity process = QProcessInstanceEntity.processInstanceEntity;
        BooleanExpression expression = process.parentId.eq(processInstanceId);
        Predicate extendedPredicate = expression;
        if (predicate != null) {
            extendedPredicate = expression.and(predicate);
        }

        return pagedCollectionModelAssembler.toModel(pageable,
                                                  processInstanceRepository.findAll(extendedPredicate,
                                                                                    pageable),
                                                  processInstanceRepresentationModelAssembler);
    }
}
