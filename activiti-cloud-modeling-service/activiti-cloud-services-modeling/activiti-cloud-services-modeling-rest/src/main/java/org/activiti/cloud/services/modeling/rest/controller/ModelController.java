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

package org.activiti.cloud.services.modeling.rest.controller;

import static org.activiti.cloud.services.common.util.HttpUtils.multipartToFileContent;
import static org.activiti.cloud.services.common.util.HttpUtils.writeFileToResponse;
import static org.activiti.cloud.services.modeling.rest.api.ProjectRestApi.EXPORT_AS_ATTACHMENT_PARAM_NAME;
import static org.activiti.cloud.services.modeling.rest.api.ProjectRestApi.UPLOAD_FILE_PARAM_NAME;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import io.swagger.annotations.ApiParam;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.modeling.rest.api.ModelRestApi;
import org.activiti.cloud.services.modeling.rest.assembler.ModelRepresentationModelAssembler;
import org.activiti.cloud.services.modeling.rest.assembler.ModelTypeRepresentationModelAssembler;
import org.activiti.cloud.services.modeling.rest.assembler.PagedModelTypeAssembler;
import org.activiti.cloud.services.modeling.service.ModelTypeService;
import org.activiti.cloud.services.modeling.service.api.ModelService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebInputException;

/**
 * Controller for {@link Model} resources
 */
@RestController
@ControllerAdvice
public class ModelController implements ModelRestApi {

    private final ModelService modelService;

    private final ModelTypeService modelTypeService;

    private final ModelRepresentationModelAssembler representationModelAssembler;

    private final AlfrescoPagedModelAssembler<Model> pagedCollectionModelAssembler;

    private final ModelTypeRepresentationModelAssembler modelTypeAssembler;

    private final PagedModelTypeAssembler pagedModelTypeAssembler;

    private final ProjectController projectController;

    public ModelController(ModelService modelService,
                           ModelTypeService modelTypeService,
                           ModelRepresentationModelAssembler representationModelAssembler,
                           AlfrescoPagedModelAssembler<Model> pagedCollectionModelAssembler,
                           ModelTypeRepresentationModelAssembler modelTypeAssembler,
                           PagedModelTypeAssembler pagedModelTypeAssembler,
                           ProjectController projectController) {
        this.modelService = modelService;
        this.modelTypeService = modelTypeService;
        this.representationModelAssembler = representationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.modelTypeAssembler = modelTypeAssembler;
        this.pagedModelTypeAssembler = pagedModelTypeAssembler;
        this.projectController = projectController;
    }

    @Override
    public PagedModel<EntityModel<Model>> getModels(
            @PathVariable String projectId,
            @RequestParam(MODEL_TYPE_PARAM_NAME) String type,
            Pageable pageable) {
        Project project = projectController.findProjectById(projectId);
        return pagedCollectionModelAssembler.toModel(
                pageable,
                modelService.getModels(project,
                                       findModelType(type),
                                       pageable),
                representationModelAssembler);
    }

    @Override
    public EntityModel<Model> getModel(
            @PathVariable String modelId) {
        return representationModelAssembler.toModel(findModelById(modelId));
    }

    @Override
    public EntityModel<Model> createModel(
            @PathVariable String projectId,
            @Valid @RequestBody Model model) {
        Project project = projectController.findProjectById(projectId);
        return representationModelAssembler.toModel(
                modelService.createModel(project,
                                         model));
    }

    @Override
    public EntityModel<Model> updateModel(
            @PathVariable String modelId,
            @Valid @RequestBody Model model) {
        Model modelToUpdate = findModelById(modelId);
        model.setId(modelId);
        return representationModelAssembler.toModel(
                modelService.updateModel(modelToUpdate,
                                         model));
    }

    @Override
    public void updateModelContent(
            @PathVariable String modelId,
            @RequestPart(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException {

        modelService.updateModelContent(findModelById(modelId),
            multipartToFileContent(file));
    }

    @Override
    public void deleteModel(
            @PathVariable String modelId) {
        modelService.deleteModel(findModelById(modelId));
    }

    @Override
    public void getModelContent(
            HttpServletResponse response,
            @PathVariable String modelId) throws IOException {
        Model model = findModelById(modelId);
        writeFileToResponse(response,
                            modelService.getModelContentFile(model),
                            false);
    }

    @Override
    public void getModelDiagram(
            HttpServletResponse response,
            @PathVariable String modelId) throws IOException {
        Model model = findModelById(modelId);
        FileContent fileContent = modelService.getModelDiagramFile(model.getId())
                .orElseThrow(() -> new NotAcceptableStatusException("Model content cannot be retrieved as svg image: " + modelId));
        writeFileToResponse(response,
                            fileContent,
                            false);
    }

    @Override
    public EntityModel<Model> importModel(
            @PathVariable String projectId,
            @RequestParam(MODEL_TYPE_PARAM_NAME) String type,
            @RequestPart(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException {
        Project project = projectController.findProjectById(projectId);
        return representationModelAssembler.toModel(
                modelService.importSingleModel(project,
                                         findModelType(type),
                                         multipartToFileContent(file)));
    }

    @Override
    public void exportModel(
            HttpServletResponse response,
            @PathVariable String modelId,
            @RequestParam(name = EXPORT_AS_ATTACHMENT_PARAM_NAME,
                    required = false,
                    defaultValue = "true") boolean attachment) throws IOException {
        Model model = findModelById(modelId);
        writeFileToResponse(response,
                            modelService.exportModel(model),
                            attachment);
    }

    @Override
    public PagedModel<EntityModel<ModelType>> getModelTypes(Pageable pageable) {
        return pagedModelTypeAssembler.toModel(pageable,
                                                  modelTypeService.getModelTypeNames(pageable),
                                                  modelTypeAssembler);
    }

    @Override
    public void validateModel(
            @PathVariable String modelId,
            @RequestParam(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException {

        modelService.validateModelContent(findModelById(modelId),
                                          multipartToFileContent(file));
    }

    @Override
    public void validateModelExtensions(
            @ApiParam(VALIDATE_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId,
            @ApiParam(VALIDATE_EXTENSIONS_FILE_PARAM_DESCR)
            @RequestParam(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException {

        modelService.validateModelExtensions(findModelById(modelId),
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
