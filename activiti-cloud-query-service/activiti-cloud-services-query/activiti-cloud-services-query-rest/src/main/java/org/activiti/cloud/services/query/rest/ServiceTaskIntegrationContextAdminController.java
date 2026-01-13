/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import jakarta.persistence.EntityNotFoundException;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudIntegrationContext;
import org.activiti.cloud.services.query.app.repository.IntegrationContextRepository;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.rest.assembler.IntegrationContextRepresentationModelAssembler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class ServiceTaskIntegrationContextAdminController {

    private final IntegrationContextRepository repository;

    private final IntegrationContextRepresentationModelAssembler representationModelAssembler;

    private final AlfrescoPagedModelAssembler<IntegrationContextEntity> pagedCollectionModelAssembler;

    public ServiceTaskIntegrationContextAdminController(
        IntegrationContextRepository repository,
        IntegrationContextRepresentationModelAssembler representationModelAssembler,
        AlfrescoPagedModelAssembler<IntegrationContextEntity> pagedCollectionModelAssembler
    ) {
        this.repository = repository;
        this.representationModelAssembler = representationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
    }

    @RequestMapping(value = "/{serviceTaskId}/integration-context", method = RequestMethod.GET)
    public EntityModel<CloudIntegrationContext> findByServiceTaskId(@PathVariable String serviceTaskId) {
        String[] split = parseServiceTaskId(serviceTaskId);

        Page<IntegrationContextEntity> page = repository.findByProcessInstanceIdAndClientIdAndExecutionId(
            split[0],
            split[1],
            split[2],
            PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "requestDate"))
        );
        if (page.hasContent()) {
            return representationModelAssembler.toModel(page.getContent().getFirst());
        } else {
            throw new EntityNotFoundException(
                "Unable to find integration context entity for the given id:'" + serviceTaskId + "'"
            );
        }
    }

    @RequestMapping(value = "/{serviceTaskId}/integration-contexts", method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudIntegrationContext>> findAllByServiceTaskId(
        @PathVariable String serviceTaskId,
        Pageable pageable
    ) {
        String[] split = parseServiceTaskId(serviceTaskId);
        return pagedCollectionModelAssembler.toModel(
            pageable,
            repository.findByProcessInstanceIdAndClientIdAndExecutionId(split[0], split[1], split[2], pageable),
            representationModelAssembler
        );
    }

    /**
     * Parses the serviceTaskId into its constituent parts.
     *
     * @param serviceTaskId the service task ID in format "processInstanceId:clientId:executionId"
     * @return an array containing [processInstanceId, clientId, executionId]
     * @throws IllegalArgumentException if the serviceTaskId format is invalid
     */
    private String[] parseServiceTaskId(String serviceTaskId) {
        String[] split = serviceTaskId.trim().split(":");
        if (split.length != 3) {
            throw new IllegalArgumentException(
                "Invalid serviceTaskId format. Expected format: 'processInstanceId:clientId:executionId', but got: '" +
                serviceTaskId +
                "'"
            );
        }
        return split;
    }
}
