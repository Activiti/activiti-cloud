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

package org.activiti.cloud.services.organization.rest.controller;

import java.util.Optional;
import java.util.stream.Collectors;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.organization.rest.assembler.ModelResourceAssembler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller for {@link Model} resources
 */
@RestController
@RequestMapping(produces = {HAL_JSON_VALUE, APPLICATION_JSON_VALUE})
public class ModelController implements ApplicationEventPublisherAware {

    private final ModelRepository modelRepository;

    private ApplicationEventPublisher applicationEventPublisher;

    private final ModelResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Model> pagedResourcesAssembler;

    private final ApplicationController applicationController;

    public ModelController(ModelRepository modelRepository,
                           ModelResourceAssembler resourceAssembler,
                           AlfrescoPagedResourcesAssembler<Model> pagedResourcesAssembler,
                           ApplicationController applicationController) {
        this.modelRepository = modelRepository;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.applicationController = applicationController;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @RequestMapping(method = GET, path = "/v1/models")
    public PagedResources<Resource<Model>> getModels(Pageable pageable) {
        return pagedResourcesAssembler.toResource(
                pageable,
                modelRepository.getTopLevelModels(pageable),
                resourceAssembler);
    }

    @RequestMapping(method = GET, path = "/v1/applications/{applicationId}/models")
    public PagedResources<Resource<Model>> getModels(@PathVariable String applicationId,
                                                     Pageable pageable) {
        Application application = applicationController.findApplicationById(applicationId);
        return pagedResourcesAssembler.toResource(
                pageable,
                modelRepository.getModels(application,
                                          pageable),
                resourceAssembler);
    }

    @RequestMapping(method = GET, path = "/v1/models/{modelId}")
    public Resource<Model> getModel(@PathVariable String modelId) {
        return resourceAssembler.toResource(findModelById(modelId));
    }

    @RequestMapping(method = POST, path = "/v1/models")
    @ResponseStatus(CREATED)
    public Resource<Model> createModel(@RequestBody Model model) {
        applicationEventPublisher.publishEvent(new BeforeCreateEvent(model));
        return resourceAssembler.toResource(
                modelRepository.createModel(model));
    }

    @RequestMapping(method = POST, path = "/v1/applications/{applicationId}/models")
    @ResponseStatus(CREATED)
    public Resource<Model> createModel(@PathVariable String applicationId,
                                       @RequestBody Model model) {
        Application application = applicationController.findApplicationById(applicationId);
        applicationEventPublisher.publishEvent(new BeforeCreateEvent(model));
        return resourceAssembler.toResource(
                modelRepository.createModel(application,
                                            model));
    }

    @RequestMapping(method = PUT, path = "/v1/models/{modelId}")
    @ResponseStatus(NO_CONTENT)
    public void updateModel(@PathVariable String modelId,
                            @RequestBody Model model) {
        Model modelToUpdate = findModelById(modelId);
        applicationEventPublisher.publishEvent(new BeforeSaveEvent(model));
        resourceAssembler.toResource(
                modelRepository.updateModel(modelToUpdate,
                                            model));
    }

    @RequestMapping(method = {PUT, PATCH}, path = "/v1/applications/{applicationId}/models", consumes = TEXT_URI_LIST_VALUE)
    @ResponseStatus(NO_CONTENT)
    public void createModelsReference(@PathVariable String applicationId,
                                      @RequestBody Resources<Object> modelsLinks) {
        modelRepository.createModelsReference(
                applicationController.findApplicationById(applicationId),
                modelsLinks
                        .getLinks()
                        .stream()
                        .map(Link::getHref)
                        .collect(Collectors.toList()));
    }

    @RequestMapping(method = DELETE, path = "/v1/models/{modelId}")
    @ResponseStatus(NO_CONTENT)
    public void deleteModel(@PathVariable String modelId) {
        modelRepository.deleteModel(findModelById(modelId));
    }

    public Model findModelById(String modelId) {
        Optional<Model> optionalModel = modelRepository.findModelById(modelId);
        return optionalModel
                .orElseThrow(() -> new ResourceNotFoundException("Organization model not found: " + modelId));
    }
}
