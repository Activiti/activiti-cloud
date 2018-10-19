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
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.ApiParam;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.organization.rest.api.ModelRestApi;
import org.activiti.cloud.services.organization.rest.assembler.ModelResourceAssembler;
import org.activiti.cloud.services.organization.rest.assembler.ModelTypeResourceAssembler;
import org.activiti.cloud.services.organization.rest.assembler.PagedModelTypeAssembler;
import org.activiti.cloud.services.organization.rest.assembler.ValidationErrorResourceAssembler;
import org.activiti.cloud.services.organization.service.ModelService;
import org.activiti.cloud.services.organization.service.ModelTypeService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebInputException;

import static org.activiti.cloud.services.common.util.HttpUtils.multipartToFileContent;
import static org.activiti.cloud.services.common.util.HttpUtils.writeFileToResponse;
import static org.activiti.cloud.services.organization.rest.api.ApplicationRestApi.EXPORT_AS_ATTACHMENT_PARAM_NAME;
import static org.activiti.cloud.services.organization.rest.api.ApplicationRestApi.UPLOAD_FILE_PARAM_NAME;
import static org.activiti.cloud.services.organization.rest.controller.ApplicationController.ATTACHMENT_API_PARAM_DESCR;

/**
 * Controller for {@link Model} resources
 */
@RestController
@ControllerAdvice
public class ModelController implements ModelRestApi {

    private final ModelService modelService;

    private final ModelTypeService modelTypeService;

    private final ModelResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Model> pagedResourcesAssembler;

    private final ModelTypeResourceAssembler modelTypeAssembler;

    private final PagedModelTypeAssembler pagedModelTypeAssembler;

    private final ValidationErrorResourceAssembler validationErrorResourceAssembler;

    private final ApplicationController applicationController;

    public ModelController(ModelService modelService,
                           ModelTypeService modelTypeService,
                           ModelResourceAssembler resourceAssembler,
                           AlfrescoPagedResourcesAssembler<Model> pagedResourcesAssembler,
                           ModelTypeResourceAssembler modelTypeAssembler,
                           PagedModelTypeAssembler pagedModelTypeAssembler,
                           ValidationErrorResourceAssembler validationErrorResourceAssembler,
                           ApplicationController applicationController) {
        this.modelService = modelService;
        this.modelTypeService = modelTypeService;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.modelTypeAssembler = modelTypeAssembler;
        this.pagedModelTypeAssembler = pagedModelTypeAssembler;
        this.validationErrorResourceAssembler = validationErrorResourceAssembler;
        this.applicationController = applicationController;
    }

    @Override
    public PagedResources<Resource<Model>> getModels(
            @ApiParam(GET_MODELS_TYPE_PARAM_DESCR)
            @RequestParam(MODEL_TYPE_PARAM_NAME) Optional<String> type,
            Pageable pageable) {
        return pagedResourcesAssembler.toResource(
                pageable,
                modelService.getTopLevelModels(type.map(this::findModelType),
                                               pageable),
                resourceAssembler);
    }

    @Override
    public PagedResources<Resource<Model>> getModels(
            @ApiParam(GET_MODELS_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(GET_MODELS_TYPE_PARAM_DESCR)
            @RequestParam(MODEL_TYPE_PARAM_NAME) Optional<String> type,
            Pageable pageable) {
        Application application = applicationController.findApplicationById(applicationId);
        return pagedResourcesAssembler.toResource(
                pageable,
                modelService.getModels(application,
                                       type.map(this::findModelType),
                                       pageable),
                resourceAssembler);
    }

    @Override
    public Resource<Model> getModel(
            @ApiParam(GET_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId) {
        return resourceAssembler.toResource(findModelById(modelId));
    }

    @Override
    public Resource<Model> createModel(
            @ApiParam(CREATE_MODEL_PARAM_DESCR)
            @RequestBody Model model) {
        return resourceAssembler.toResource(
                modelService.createModel(null,
                                         model));
    }

    @Override
    public Resource<Model> createModel(
            @ApiParam(CREATE_MODEL_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(CREATE_MODEL_PARAM_DESCR)
            @RequestBody Model model) {
        Application application = applicationController.findApplicationById(applicationId);
        return resourceAssembler.toResource(
                modelService.createModel(application,
                                         model));
    }

    @Override
    public Resource<Model> updateModel(
            @ApiParam(UPDATE_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId,
            @ApiParam(UPDATE_MODEL_PARAM_DESCR)
            @RequestBody Model model) {
        Model modelToUpdate = findModelById(modelId);
        model.setId(modelId);
        return resourceAssembler.toResource(
                modelService.updateModel(modelToUpdate,
                                         model));
    }

    @Override
    public void updateModelContent(
            @ApiParam(UPDATE_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId,
            @ApiParam(UPDATE_MODEL_FILE_PARAM_DESCR)
            @RequestPart(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException {
        modelService.updateModelContent(findModelById(modelId),
                                        multipartToFileContent(file));
    }

    @Override
    public void deleteModel(
            @ApiParam(DELETE_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId) {
        modelService.deleteModel(findModelById(modelId));
    }

    @Override
    public void getModelContent(
            HttpServletResponse response,
            @ApiParam(GET_MODEL_CONTENT_ID_PARAM_DESCR)
            @PathVariable String modelId) throws IOException {
        Model model = findModelById(modelId);
        writeFileToResponse(response,
                            modelService.getModelContent(model),
                            false);
    }

    @Override
    public void getModelDiagram(
            HttpServletResponse response,
            @ApiParam(GET_MODEL_CONTENT_ID_PARAM_DESCR)
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
            @ApiParam(CREATE_MODEL_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(IMPORT_MODEL_TYPE_PARAM_DESCR)
            @RequestParam(MODEL_TYPE_PARAM_NAME) String type,
            @ApiParam(IMPORT_MODEL_FILE_PARAM_DESCR)
            @RequestPart(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException {
        Application application = applicationController.findApplicationById(applicationId);
        return resourceAssembler.toResource(
                modelService.importModel(application,
                                         findModelType(type),
                                         multipartToFileContent(file)));
    }

    @Override
    public void exportModel(
            HttpServletResponse response,
            @ApiParam(EXPORT_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId,
            @ApiParam(ATTACHMENT_API_PARAM_DESCR)
            @RequestParam(name = EXPORT_AS_ATTACHMENT_PARAM_NAME,
                    required = false,
                    defaultValue = "true") boolean attachment) throws IOException {
        Model model = findModelById(modelId);
        writeFileToResponse(response,
                            modelService.exportModel(model),
                            attachment);
    }

    @Override
    public PagedResources<Resource<ModelType>> getModelTypes(Pageable pageable) {
        return pagedModelTypeAssembler.toResource(pageable,
                                                  modelTypeService.getModelTypeNames(pageable),
                                                  modelTypeAssembler);
    }

    @Override
    public void validateModel(
            @ApiParam(VALIDATE_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId,
            @ApiParam(VALIDATE_MODEL_FILE_PARAM_DESCR)
            @RequestParam(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException {

        Model model = modelService.findModelById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model not found: " + modelId));
        modelService.validateModelContent(model,
                                          multipartToFileContent(file));
    }

    public Model findModelById(String modelId) {
        Optional<Model> optionalModel = modelService.findModelById(modelId);
        return optionalModel
                .orElseThrow(() -> new ResourceNotFoundException("Model not found: " + modelId));
    }

    public ModelType findModelType(String type) {
        Optional<ModelType> optionalModelType = modelTypeService.findModelTypeByName(type);
        return optionalModelType
                .orElseThrow(() -> new ServerWebInputException("Unknown model type: " + type));
    }
}
