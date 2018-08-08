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

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.ApiParam;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.organization.rest.api.ModelRestApi;
import org.activiti.cloud.services.organization.rest.assembler.ModelResourceAssembler;
import org.activiti.cloud.services.organization.rest.assembler.ValidationErrorResourceAssembler;
import org.activiti.cloud.services.organization.rest.resource.ValidationErrorResource;
import org.activiti.cloud.services.organization.service.ModelService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.NotAcceptableStatusException;

import static org.activiti.cloud.services.common.util.HttpUtils.multipartToFileContent;
import static org.activiti.cloud.services.common.util.HttpUtils.writeFileToResponse;
import static org.activiti.cloud.services.organization.swagger.SwaggerConfiguration.ATTACHEMNT_API_PARAM_DESCRIPTION;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * Controller for {@link Model} resources
 */
@RestController
public class ModelController implements ModelRestApi {

    private final ModelService modelService;

    private final ModelResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Model> pagedResourcesAssembler;

    private final ValidationErrorResourceAssembler validationErrorResourceAssembler;

    private final ApplicationController applicationController;

    public ModelController(ModelService modelService,
                           ModelResourceAssembler resourceAssembler,
                           AlfrescoPagedResourcesAssembler<Model> pagedResourcesAssembler,
                           ValidationErrorResourceAssembler validationErrorResourceAssembler,
                           ApplicationController applicationController) {
        this.modelService = modelService;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.validationErrorResourceAssembler = validationErrorResourceAssembler;
        this.applicationController = applicationController;
    }

    @Override
    public PagedResources<Resource<Model>> getModels(
            @ApiParam("The type of the model to filter")
            @RequestParam(value = "type", required = false) ModelType type,
            Pageable pageable) {
        return pagedResourcesAssembler.toResource(
                pageable,
                modelService.getTopLevelModels(type,
                                               pageable),
                resourceAssembler);
    }

    @Override
    public PagedResources<Resource<Model>> getModels(
            @ApiParam("The id of the application to get the models for")
            @PathVariable String applicationId,
            @ApiParam("The type of the model to filter")
            @RequestParam(value = "type", required = false) ModelType type,
            Pageable pageable) {
        Application application = applicationController.findApplicationById(applicationId);
        return pagedResourcesAssembler.toResource(
                pageable,
                modelService.getModels(application,
                                       type,
                                       pageable),
                resourceAssembler);
    }

    @Override
    public Resource<Model> getModel(
            @ApiParam("The id of the model to retrieve")
            @PathVariable String modelId) {
        return resourceAssembler.toResource(findModelById(modelId));
    }

    @Override
    public Resource<Model> createModel(
            @ApiParam("The details of the model to create")
            @RequestBody Model model) {
        return resourceAssembler.toResource(
                modelService.createModel(null,
                                         model));
    }

    @Override
    public Resource<Model> createModel(
            @ApiParam("The id of the application to associate the new model with")
            @PathVariable String applicationId,
            @ApiParam("The details of the model to create")
            @RequestBody Model model) {
        Application application = applicationController.findApplicationById(applicationId);
        return resourceAssembler.toResource(
                modelService.createModel(application,
                                         model));
    }

    @Override
    public Resource<Model> updateModel(
            @ApiParam("The id of the model to update")
            @PathVariable String modelId,
            @ApiParam("The new values to update")
            @RequestBody Model model) {
        Model modelToUpdate = findModelById(modelId);
        model.setId(modelId);
        return resourceAssembler.toResource(
                modelService.updateModel(modelToUpdate,
                                         model));
    }

    @Override
    public void updateModelContent(
            @ApiParam("The id of the model to update")
            @PathVariable String modelId,
            @ApiParam("The file containing the model content")
            @RequestPart("file") MultipartFile file) throws IOException {
        modelService.updateModelContent(findModelById(modelId),
                                        multipartToFileContent(file));
    }

    @Override
    public void deleteModel(
            @ApiParam("The id of the model to delete")
            @PathVariable String modelId) {
        modelService.deleteModel(findModelById(modelId));
    }

    @Override
    public void getModelContent(
            HttpServletResponse response,
            @ApiParam("The id of the model to get the content")
            @PathVariable String modelId) throws IOException {
        Model model = findModelById(modelId);
        FileContent fileContent = modelService.getModelContent(model.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Model content not found: " + modelId));
        writeFileToResponse(response,
                            fileContent,
                            false);
    }

    @Override
    public void getModelDiagram(
            HttpServletResponse response,
            @ApiParam("The id of the model to get the content")
            @PathVariable String modelId) throws IOException {
        Model model = findModelById(modelId);
        FileContent fileContent = modelService.getModelDiagram(model.getId())
                .orElseThrow(() -> new NotAcceptableStatusException("Model content cannot be retrieved as svg image: " + modelId));
        writeFileToResponse(response,
                            fileContent,
                            false);
    }

    @Override
    public Resource<Model> importModel(
            @ApiParam("The id of the application to associate the new model with")
            @PathVariable String applicationId,
            @ApiParam("The type of the model to be imported")
            @RequestParam(value = "type", required = false) ModelType type,
            @ApiParam("The file containing the model definition")
            @RequestPart("file") MultipartFile file) throws IOException {
        Application application = applicationController.findApplicationById(applicationId);
        return resourceAssembler.toResource(
                modelService.importModel(application,
                                         type,
                                         multipartToFileContent(file)));
    }

    @Override
    public void exportModel(
            HttpServletResponse response,
            @ApiParam("The id of the model to export")
            @PathVariable String modelId,
            @ApiParam(ATTACHEMNT_API_PARAM_DESCRIPTION)
            @RequestParam(name = "attachment",
                    required = false,
                    defaultValue = "true") boolean attachment) throws IOException {
        Model model = findModelById(modelId);
        Optional<FileContent> fileContent = modelService.exportModel(model.getId());
        if (fileContent.isPresent()) {
            writeFileToResponse(response,
                                fileContent.get(),
                                attachment);
        }
    }

    @Override
    public Resources<ValidationErrorResource> validateModel(
            @ApiParam("The id of the model to validate the content for")
            @PathVariable(value = "modelId") String modelId,
            @ApiParam("The file containing the model definition to validate")
            @RequestParam("file") MultipartFile file) throws IOException {

        FileContent fileContent = multipartToFileContent(file);
        Optional<Model> optionalModel = modelService.findModelById(modelId);
        List<ModelValidationError> validationResult = optionalModel
                .map(model -> modelService.validateModelContent(model,
                                                                fileContent))
                .orElseThrow(() -> new ResourceNotFoundException("Model not found: " + modelId));

        return new Resources<>(validationErrorResourceAssembler.toResources(validationResult),
                               linkTo(ModelController.class).withSelfRel());
    }

    public Model findModelById(String modelId) {
        Optional<Model> optionalModel = modelService.findModelById(modelId);
        return optionalModel
                .orElseThrow(() -> new ResourceNotFoundException("Model not found: " + modelId));
    }
}
