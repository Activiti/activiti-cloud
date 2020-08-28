/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.rest;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.QBPMNActivityEntity;
import org.activiti.cloud.services.query.rest.assembler.ServiceTaskRepresentationModelAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.querydsl.core.types.Predicate;

@RestController
@RequestMapping(
        value = "/admin/v1/service-tasks",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class ServiceTaskAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskAdminController.class);

    private final BPMNActivityRepository bpmnActivityRepository;

    private ServiceTaskRepresentationModelAssembler taskRepresentationModelAssembler;

    private AlfrescoPagedModelAssembler<BPMNActivityEntity> pagedCollectionModelAssembler;

    private EntityFinder entityFinder;


    public ServiceTaskAdminController(BPMNActivityRepository bpmnActivityRepository,
                                      ServiceTaskRepresentationModelAssembler taskRepresentationModelAssembler,
                                      AlfrescoPagedModelAssembler<BPMNActivityEntity> pagedCollectionModelAssembler,
                                      EntityFinder entityFinder) {
        this.bpmnActivityRepository = bpmnActivityRepository;
        this.taskRepresentationModelAssembler = taskRepresentationModelAssembler;
        this.entityFinder = entityFinder;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudBPMNActivity>> findAll(@QuerydslPredicate(root = BPMNActivityEntity.class) Predicate predicate,
                                                              Pageable pageable) {

        Predicate filter = QBPMNActivityEntity.bPMNActivityEntity.activityType.eq("serviceTask").and(predicate);

        return pagedCollectionModelAssembler.toModel(pageable,
                                                     bpmnActivityRepository.findAll(filter,
                                                                                    pageable),
                                                     taskRepresentationModelAssembler);
    }

    @RequestMapping(value = "/{serviceTaskId}", method = RequestMethod.GET)
    public EntityModel<CloudBPMNActivity> findById(@PathVariable String serviceTaskId) {

        BPMNActivityEntity entity = entityFinder.findById(bpmnActivityRepository,
                                                          serviceTaskId,
                                                          "Unable to find service task entity for the given id:'" + serviceTaskId + "'");

        return taskRepresentationModelAssembler.toModel(entity);
    }
}
