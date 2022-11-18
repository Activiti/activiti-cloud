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

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import java.util.Optional;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudApplication;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.activiti.cloud.services.query.rest.assembler.ApplicationRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/applications", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
public class ApplicationController {

    private ApplicationRepository repository;
    private AlfrescoPagedModelAssembler<ApplicationEntity> pagedCollectionModelAssembler;
    private ApplicationRepresentationModelAssembler applicationRepresentationModelAssembler;

    @Autowired
    public ApplicationController(
        ApplicationRepository repository,
        AlfrescoPagedModelAssembler<ApplicationEntity> pagedCollectionModelAssembler,
        ApplicationRepresentationModelAssembler applicationRepresentationModelAssembler
    ) {
        this.repository = repository;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.applicationRepresentationModelAssembler = applicationRepresentationModelAssembler;
    }

    @GetMapping
    public PagedModel<EntityModel<CloudApplication>> findAll(
        @QuerydslPredicate(root = ApplicationEntity.class) Predicate predicate,
        Pageable pageable
    ) {
        predicate = Optional.ofNullable(predicate).orElseGet(BooleanBuilder::new);

        return pagedCollectionModelAssembler.toModel(
            pageable,
            repository.findAll(predicate, pageable),
            applicationRepresentationModelAssembler
        );
    }
}
