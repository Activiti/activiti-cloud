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

package org.activiti.cloud.services.organization.rest.api;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_SVG;
import static org.activiti.cloud.services.organization.rest.api.ModelRestApi.MODELS;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.activiti.cloud.services.organization.rest.controller.ApplicationController.ATTACHMENT_API_PARAM_DESCR;
import static org.activiti.cloud.services.organization.rest.controller.ApplicationController.EXPORT_AS_ATTACHMENT_PARAM_NAME;
import static org.activiti.cloud.services.organization.rest.controller.ApplicationController.UPLOAD_FILE_PARAM_NAME;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Controller for process resources.
 */
@RestController
@Api(tags = MODELS, description = "Retrieve and manage models")
@RequestMapping(path = API_VERSION, produces = {HAL_JSON_VALUE, APPLICATION_JSON_VALUE})
public interface ModelRestApi {

    String MODELS = "models";

    String GET_MODELS_TYPE_PARAM_DESCR = "The type of the model to filter";

    String GET_MODELS_APPLICATION_ID_PARAM_DESCR = "The id of the application to get the models for";

    String GET_MODEL_ID_PARAM_DESCR = "The id of the model to retrieve";

    String CREATE_MODEL_PARAM_DESCR = "The details of the model to create";

    String CREATE_MODEL_APPLICATION_ID_PARAM_DESCR = "The id of the application to associate the new model with";

    String UPDATE_MODEL_ID_PARAM_DESCR = "The id of the model to update";

    String UPDATE_MODEL_PARAM_DESCR = "The new values to update";

    String UPDATE_MODEL_FILE_PARAM_DESCR = "The file containing the model content";

    String DELETE_MODEL_ID_PARAM_DESCR = "The id of the model to delete";

    String GET_MODEL_CONTENT_ID_PARAM_DESCR = "The id of the model to get the content";

    String IMPORT_MODEL_TYPE_PARAM_DESCR = "The type of the model to be imported";

    String IMPORT_MODEL_FILE_PARAM_DESCR = "The file containing the model definition";

    String EXPORT_MODEL_ID_PARAM_DESCR = "The id of the model to export";

    String VALIDATE_MODEL_ID_PARAM_DESCR = "The id of the model to validate the content for";

    String VALIDATE_MODEL_FILE_PARAM_DESCR = "The file containing the model definition to validate";

    String MODEL_TYPE_PARAM_NAME = "type";

    @ApiOperation(
            tags = MODELS,
            value = "List standalone models",
            notes = "Get the standalone models. " +
                    "Minimal information for each model is returned."
            //response = AlfrescoModelPage.class
    )
    @RequestMapping(method = GET, path = "/models")
    PagedResources<Resource<Model>> getModels(
            @ApiParam(GET_MODELS_TYPE_PARAM_DESCR)
            @RequestParam(MODEL_TYPE_PARAM_NAME) Optional<String> type,
            Pageable pageable);

    @ApiOperation(
            tags = MODELS,
            value = "List models for an application",
            notes = "Get the models associated with an application. " +
                    "Minimal information for each model is returned."
            //response = AlfrescoModelPage.class
    )
    @GetMapping(path = "/applications/{applicationId}/models")
    PagedResources<Resource<Model>> getModels(
            @ApiParam(GET_MODELS_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(GET_MODELS_TYPE_PARAM_DESCR)
            @RequestParam(MODEL_TYPE_PARAM_NAME) Optional<String> type,
            Pageable pageable);

    @ApiOperation(
            tags = MODELS,
            value = "Get metadata information for a model")
    @GetMapping(path = "/models/{modelId}")
    Resource<Model> getModel(
            @ApiParam(GET_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId);

    @ApiOperation(
            tags = MODELS,
            value = "Create new standalone model",
            notes = "Create a new standalone model")
    @PostMapping(path = "/models")
    @ResponseStatus(CREATED)
    Resource<Model> createModel(
            @ApiParam(CREATE_MODEL_PARAM_DESCR)
            @RequestBody Model model);

    @ApiOperation(
            tags = MODELS,
            value = "Create new model belonging to an application",
            notes = "Create a new model related to an existing application")
    @PostMapping(path = "/applications/{applicationId}/models")
    @ResponseStatus(CREATED)
    Resource<Model> createModel(
            @ApiParam(CREATE_MODEL_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(CREATE_MODEL_PARAM_DESCR)
            @RequestBody Model model);

    @ApiOperation(
            tags = MODELS,
            value = "Update model metadata",
            notes = "Update the details of a model.")
    @PutMapping(path = "/models/{modelId}")
    Resource<Model> updateModel(
            @ApiParam(UPDATE_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId,
            @ApiParam(UPDATE_MODEL_PARAM_DESCR)
            @RequestBody Model model);

    @ApiOperation(
            tags = MODELS,
            value = "Update model content",
            notes = "Update the content of the model from file.")
    @PutMapping(path = "/models/{modelId}/content")
    @ResponseStatus(NO_CONTENT)
    void updateModelContent(
            @ApiParam(UPDATE_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId,
            @ApiParam(UPDATE_MODEL_FILE_PARAM_DESCR)
            @RequestPart(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException;

    @ApiOperation(
            tags = MODELS,
            value = "Delete model")
    @DeleteMapping(path = "/models/{modelId}")
    @ResponseStatus(NO_CONTENT)
    void deleteModel(
            @ApiParam(DELETE_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId);

    @ApiOperation(
            tags = MODELS,
            value = "Get the model content",
            notes = "Retrieve the content of the model for the identifier <b>modelId</b> " +
                    "with the content type corresponding to the model type " +
                    "(xml for process models and json for the others).<br>" +
                    "For <b>Accept: image/svg+xml</b> request header, " +
                    "the svg image corresponding to the model content will be retrieved.")
    @GetMapping(path = "/models/{modelId}/content")
    void getModelContent(
            HttpServletResponse response,
            @ApiParam(GET_MODEL_CONTENT_ID_PARAM_DESCR)
            @PathVariable String modelId) throws IOException;

    @GetMapping(path = "/models/{modelId}/content", produces = CONTENT_TYPE_SVG)
    void getModelDiagram(
            HttpServletResponse response,
            @ApiParam(GET_MODEL_CONTENT_ID_PARAM_DESCR)
            @PathVariable String modelId) throws IOException;

    @ApiOperation(
            tags = MODELS,
            value = "Import a model from file",
            notes = "Allows a file to be uploaded containing a model definition.")
    @PostMapping(path = "/applications/{applicationId}/models/import", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(CREATED)
    Resource<Model> importModel(
            @ApiParam(CREATE_MODEL_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(IMPORT_MODEL_TYPE_PARAM_DESCR)
            @RequestParam(MODEL_TYPE_PARAM_NAME) String type,
            @ApiParam(IMPORT_MODEL_FILE_PARAM_DESCR)
            @RequestPart(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException;

    @ApiOperation(
            tags = MODELS,
            value = "Export a model definition as file",
            notes = "Allows to download a file containing a model metadata along with the model content.")
    @GetMapping(path = "/models/{modelId}/export")
    void exportModel(
            HttpServletResponse response,
            @ApiParam(EXPORT_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId,
            @ApiParam(ATTACHMENT_API_PARAM_DESCR)
            @RequestParam(name = EXPORT_AS_ATTACHMENT_PARAM_NAME,
                    required = false,
                    defaultValue = "true") boolean attachment) throws IOException;

    @ApiOperation(
            tags = MODELS,
            value = "List model types",
            notes = "Get the list of available model types.")
    @GetMapping(path = "/model-types")
    PagedResources<Resource<ModelType>> getModelTypes(Pageable pageable);

    @ApiOperation(
            tags = MODELS,
            value = "Validate a model content",
            notes = "Allows to the model content without save it.")
    @PostMapping("/models/{modelId}/validate")
    @ResponseStatus(NO_CONTENT)
    void validateModel(
            @ApiParam(VALIDATE_MODEL_ID_PARAM_DESCR)
            @PathVariable String modelId,
            @ApiParam(VALIDATE_MODEL_FILE_PARAM_DESCR)
            @RequestParam(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException;
}
