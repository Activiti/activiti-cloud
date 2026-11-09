/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import static org.activiti.cloud.services.query.rest.RestDocConstants.PREDICATE_DESC;
import static org.activiti.cloud.services.query.rest.RestDocConstants.PREDICATE_EXAMPLE;

import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Parameter;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudApplication;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.activiti.cloud.services.query.rest.assembler.ApplicationRepresentationModelAssembler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * V2 of the applications admin endpoint.
 * <p>
 * Unlike v1, results are fetched without a {@code COUNT} query, sorted by {@code version} descending and paginated
 * with a page size of 10 by default. The page metadata reports {@code hasMoreItems}, while {@code totalItems}
 * is only an estimate derived from the current offset.
 */
@RestController
@ExposesResourceFor(ApplicationEntity.class)
@RequestMapping(
    value = "/admin/v2/applications",
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public class ApplicationAdminControllerV2 {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ApplicationAdminService applicationAdminService;
    private final AlfrescoPagedModelAssembler<ApplicationEntity> pagedCollectionModelAssembler;
    private final ApplicationRepresentationModelAssembler applicationRepresentationModelAssembler;

    public ApplicationAdminControllerV2(
        ApplicationAdminService applicationAdminService,
        AlfrescoPagedModelAssembler<ApplicationEntity> pagedCollectionModelAssembler,
        ApplicationRepresentationModelAssembler applicationRepresentationModelAssembler
    ) {
        this.applicationAdminService = applicationAdminService;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.applicationRepresentationModelAssembler = applicationRepresentationModelAssembler;
    }

    @GetMapping
    public PagedModel<EntityModel<CloudApplication>> findAllApplicationAdmin(
        @Parameter(description = PREDICATE_DESC, example = PREDICATE_EXAMPLE) @QuerydslPredicate(
            root = ApplicationEntity.class
        ) Predicate predicate,
        @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        Page<ApplicationEntity> page = applicationAdminService.findAllSliced(predicate, pageable);

        return pagedCollectionModelAssembler.toModel(pageable, page, applicationRepresentationModelAssembler);
    }
}
