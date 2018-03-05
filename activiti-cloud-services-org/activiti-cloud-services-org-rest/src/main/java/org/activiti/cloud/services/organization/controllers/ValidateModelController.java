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

package org.activiti.cloud.services.organization.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.activiti.cloud.organization.core.model.Model;
import org.activiti.cloud.organization.core.rest.context.RestContext;
import org.activiti.cloud.organization.core.rest.context.RestContextProvider;
import org.activiti.cloud.organization.core.rest.context.RestResourceContextItem;
import org.activiti.cloud.organization.core.service.RestClientService;
import org.activiti.cloud.organization.core.service.ValidationErrorRepresentation;
import org.activiti.cloud.services.organization.assemblers.ValidationErrorResourceAssembler;
import org.activiti.cloud.services.organization.jpa.ModelRepository;
import org.activiti.cloud.services.organization.resources.ValidationErrorResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.activiti.cloud.services.organization.config.RepositoryRestConfig.API_VERSION;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RestController
@RequestMapping(value = API_VERSION)
public class ValidateModelController {

    private final RestClientService restClientService;
    private final RestContextProvider contextProvider;

    private final ModelRepository modelRepository;
    private final ValidationErrorResourceAssembler validationErrorResourceAssembler;

    @Autowired
    public ValidateModelController(RestClientService restClientService,
                                   RestContextProvider contextProvider,
                                   ModelRepository modelRepository,
                                   ValidationErrorResourceAssembler validationErrorResourceAssembler) {
        this.restClientService = restClientService;
        this.contextProvider = contextProvider;
        this.modelRepository = modelRepository;
        this.validationErrorResourceAssembler = validationErrorResourceAssembler;
    }

    @RequestMapping(value = "/models/{modelId}/validate", method = RequestMethod.POST, produces = MediaTypes.HAL_JSON_VALUE)
    public Resources<ValidationErrorResource> validateModel(@PathVariable(value = "modelId") String modelId,
                                                            @RequestParam("file") MultipartFile content) throws IOException {

        Optional<Model> model = modelRepository.findById(modelId);
        if (!model.isPresent()) {
            throw new ResourceNotFoundException();
        }

        return new Resources<>(validationErrorResourceAssembler.toResources(validateModel(model.get(),
                                                                                          content)),
                               linkTo(ValidateModelController.class).withSelfRel());
    }

    private List<ValidationErrorRepresentation> validateModel(Model processModel,
                                                              MultipartFile multipartFile) throws IOException {
        // todo refactor this to a service separately
        final RestResourceContextItem restContextItem = contextProvider
                .getContext(RestContext.ACTIVITI)
                .getResource(processModel.getType());
        return restClientService
                .validateModel(new StringBuilder(restContextItem.getUrl())
                                       .append(API_VERSION)
                                       .append("/")
                                       .append(restContextItem.getName())
                                       .append("/validate")
                                       .toString(),
                               multipartFile.getOriginalFilename(),
                               multipartFile.getBytes());

    }
}
