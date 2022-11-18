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

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudServiceTask;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ServiceTaskRepository;
import org.activiti.cloud.services.query.model.QServiceTaskEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.activiti.cloud.services.query.rest.assembler.ServiceTaskRepresentationModelAssembler;
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

@RestController
@RequestMapping(
    value = "/admin/v1/service-tasks",
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public class ServiceTaskAdminController {

    private final ServiceTaskRepository serviceTaskRepository;

    private final ServiceTaskRepresentationModelAssembler representationModelAssembler;

    private final AlfrescoPagedModelAssembler<ServiceTaskEntity> pagedCollectionModelAssembler;

    private final EntityFinder entityFinder;

    public ServiceTaskAdminController(
        ServiceTaskRepository serviceTaskRepository,
        ServiceTaskRepresentationModelAssembler representationModelAssembler,
        AlfrescoPagedModelAssembler<ServiceTaskEntity> pagedCollectionModelAssembler,
        EntityFinder entityFinder
    ) {
        this.serviceTaskRepository = serviceTaskRepository;
        this.representationModelAssembler = representationModelAssembler;
        this.entityFinder = entityFinder;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudServiceTask>> findAll(
        @QuerydslPredicate(root = ServiceTaskEntity.class) Predicate predicate,
        Pageable pageable
    ) {
        return pagedCollectionModelAssembler.toModel(
            pageable,
            serviceTaskRepository.findAll(predicate, pageable),
            representationModelAssembler
        );
    }

    @RequestMapping(value = "/{serviceTaskId}", method = RequestMethod.GET)
    public EntityModel<CloudServiceTask> findById(@PathVariable String serviceTaskId) {
        Predicate filter = QServiceTaskEntity.serviceTaskEntity.id.eq(serviceTaskId);

        ServiceTaskEntity entity = entityFinder.findOne(
            serviceTaskRepository,
            filter,
            "Unable to find service task entity for the given id:'" + serviceTaskId + "'"
        );

        return representationModelAssembler.toModel(entity);
    }
}
