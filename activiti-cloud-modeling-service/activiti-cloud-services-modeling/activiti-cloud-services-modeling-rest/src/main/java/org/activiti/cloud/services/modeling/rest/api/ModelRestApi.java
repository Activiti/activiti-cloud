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
package org.activiti.cloud.services.modeling.rest.api;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_SVG;
import static org.activiti.cloud.services.modeling.rest.api.ModelRestApi.MODELS;
import static org.activiti.cloud.services.modeling.rest.controller.ProjectController.ATTACHMENT_API_PARAM_DESCR;
import static org.activiti.cloud.services.modeling.rest.controller.ProjectController.EXPORT_AS_ATTACHMENT_PARAM_NAME;
import static org.activiti.cloud.services.modeling.rest.controller.ProjectController.UPLOAD_FILE_PARAM_NAME;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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

/**
 * Controller for process resources.
 */
@RestController
@Tag(name = MODELS, description = "Retrieve and manage models")
@RequestMapping(path = "/v1", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
public interface ModelRestApi {
    String MODELS = "models";

    String GET_MODELS_TYPE_PARAM_DESCR = "The type of the model to filter";

    String GET_MODELS_NAME_PARAM_DESCR = "The name or part of the name of the model to filter";

    String GET_MODELS_PROJECT_ID_PARAM_DESCR = "The id of the project to get the models for";

    String GET_MODEL_ID_PARAM_DESCR = "The id of the model to retrieve";

    String CREATE_MODEL_PARAM_DESCR = "The details of the model to create";

    String CREATE_MODEL_PROJECT_ID_PARAM_DESCR = "The id of the project to associate the new model with";

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

    String VALIDATE_EXTENSIONS_FILE_PARAM_DESCR = "The file containing the model extensions to validate";

    String MODEL_TYPE_PARAM_NAME = "type";

    String MODEL_NAME_PARAM_NAME = "name";

    String VALIDATE_PROJECT_ID_PARAM_DESCR =
        "The id of the project in whose context the model is going to be validated";

    String PROJECT_ID_PARAM_NAME = "projectId";

    String MODEL_USED_PARAM_DESCR = "The model is going to be validated and checked used in other model";

    String MODEL_USED_PARAM_NAME = "validateUsage";

    String INCLUDE_ORPHANS_PARAM_DESCR =
        "If true, then models with no relationship to any project are retrieved regardless of their scope";

    String INCLUDE_ORPHANS_PARAM_NAME = "includeOrphans";

    String RELATE_MODEL_PROJECT_PROJECT_ID_PARAM_DESCR = "The id of the project to associate the model with";

    String RELATE_MODEL_PROJECT_MODEL_ID_PARAM_DESCR = "The id of the model to associate the project with";

    String SCOPE_PARAM_DESCR = "Scope to update the model if needed (optional)";

    String SCOPE_PARAM_NAME = "scope";

    String FORCE_PARAM_DESCR =
        "If the scope of the model has restrictions on the number of projects that a model can belong to, remove the other relationships of the model with other projects";

    String FORCE_PARAM_NAME = "force";

    String DELETE_RELATIONSHIP_MODEL_PROJECT_PROJECT_ID_PARAM_DESCR =
        "The id of the project of the relationship to delete";

    String DELETE_RELATIONSHIP_MODEL_PROJECT_MODEL_ID_PARAM_DESCR = "The id of the model of the relationship to delete";

    @Operation(
        tags = MODELS,
        summary = "List models for an project",
        description = "Get the models associated with an project. Minimal information for each model is returned."
    )
    @GetMapping(path = "/projects/{projectId}/models")
    PagedModel<EntityModel<Model>> getModels(
        @Parameter(description = GET_MODELS_PROJECT_ID_PARAM_DESCR, required = true) @PathVariable String projectId,
        @Parameter(description = GET_MODELS_TYPE_PARAM_DESCR) @RequestParam(MODEL_TYPE_PARAM_NAME) String type,
        Pageable pageable
    );

    @Operation(
        tags = MODELS,
        summary = "List models for an project searching by name",
        description = "Get the models associated with an project searching by part of the name ignoring case. " +
        "Minimal information for each model is returned."
    )
    @GetMapping(path = "/projects/{projectId}/models/findByName")
    PagedModel<EntityModel<Model>> getModelsByName(
        @Parameter(description = GET_MODELS_PROJECT_ID_PARAM_DESCR, required = true) @PathVariable String projectId,
        @Parameter(description = GET_MODELS_NAME_PARAM_DESCR) @RequestParam(MODEL_NAME_PARAM_NAME) String name,
        Pageable pageable
    );

    @Operation(tags = MODELS, summary = "Get metadata information for a model")
    @GetMapping(path = "/models/{modelId}")
    EntityModel<Model> getModel(
        @Parameter(description = GET_MODEL_ID_PARAM_DESCR, required = true) @PathVariable String modelId
    );

    @Operation(
        tags = MODELS,
        summary = "Create new model belonging to an project",
        description = "Create a new model related to an existing project"
    )
    @PostMapping(path = "/projects/{projectId}/models")
    @ResponseStatus(CREATED)
    EntityModel<Model> createModel(
        @Parameter(description = CREATE_MODEL_PROJECT_ID_PARAM_DESCR, required = true) @PathVariable String projectId,
        @Parameter(description = CREATE_MODEL_PARAM_DESCR) @RequestBody Model model
    );

    @Operation(tags = MODELS, summary = "Update model metadata", description = "Update the details of a model.")
    @PutMapping(path = "/models/{modelId}")
    EntityModel<Model> updateModel(
        @Parameter(description = UPDATE_MODEL_ID_PARAM_DESCR, required = true) @PathVariable String modelId,
        @Parameter(description = UPDATE_MODEL_PARAM_DESCR) @RequestBody Model model
    );

    @Operation(
        tags = MODELS,
        summary = "Update model content",
        description = "Update the content of the model from file."
    )
    @PutMapping(path = "/models/{modelId}/content", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(NO_CONTENT)
    void updateModelContent(
        @Parameter(description = UPDATE_MODEL_ID_PARAM_DESCR, required = true) @PathVariable String modelId,
        @Parameter(description = UPDATE_MODEL_FILE_PARAM_DESCR) @RequestPart(UPLOAD_FILE_PARAM_NAME) MultipartFile file
    ) throws IOException;

    @Operation(tags = MODELS, summary = "Delete model")
    @DeleteMapping(path = "/models/{modelId}")
    @ResponseStatus(NO_CONTENT)
    void deleteModel(
        @Parameter(description = DELETE_MODEL_ID_PARAM_DESCR, required = true) @PathVariable String modelId
    );

    @Operation(
        tags = MODELS,
        summary = "Get the model content",
        description = "Retrieve the content of the model for the identifier <b>modelId</b> " +
        "with the content type corresponding to the model type " +
        "(xml for process models and json for the others).<br>" +
        "For <b>Accept: image/svg+xml</b> request header, " +
        "the svg image corresponding to the model content will be retrieved."
    )
    @GetMapping(path = "/models/{modelId}/content")
    void getModelContent(
        HttpServletResponse response,
        @Parameter(description = GET_MODEL_CONTENT_ID_PARAM_DESCR, required = true) @PathVariable String modelId
    ) throws IOException;

    @GetMapping(path = "/models/{modelId}/content", produces = CONTENT_TYPE_SVG)
    void getModelDiagram(
        HttpServletResponse response,
        @Parameter(description = GET_MODEL_CONTENT_ID_PARAM_DESCR, required = true) @PathVariable String modelId
    ) throws IOException;

    @Operation(
        tags = MODELS,
        summary = "Import a model from file",
        description = "Allows a file to be uploaded containing a model definition."
    )
    @PostMapping(path = "/projects/{projectId}/models/import", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(CREATED)
    EntityModel<Model> importModel(
        @Parameter(description = CREATE_MODEL_PROJECT_ID_PARAM_DESCR, required = true) @PathVariable String projectId,
        @Parameter(description = IMPORT_MODEL_TYPE_PARAM_DESCR) @RequestParam(MODEL_TYPE_PARAM_NAME) String type,
        @Parameter(description = IMPORT_MODEL_FILE_PARAM_DESCR) @RequestPart(UPLOAD_FILE_PARAM_NAME) MultipartFile file
    ) throws IOException;

    @Operation(
        tags = MODELS,
        summary = "Export a model definition as file",
        description = "Allows to download a file containing a model metadata along with the model content."
    )
    @GetMapping(path = "/models/{modelId}/export")
    void exportModel(
        HttpServletResponse response,
        @Parameter(description = EXPORT_MODEL_ID_PARAM_DESCR, required = true) @PathVariable String modelId,
        @Parameter(description = ATTACHMENT_API_PARAM_DESCR) @RequestParam(
            name = EXPORT_AS_ATTACHMENT_PARAM_NAME,
            required = false,
            defaultValue = "true"
        ) boolean attachment
    ) throws IOException;

    @Operation(tags = MODELS, summary = "List model types", description = "Get the list of available model types.")
    @GetMapping(path = "/model-types")
    PagedModel<EntityModel<ModelType>> getModelTypes(Pageable pageable);

    @Operation(
        tags = MODELS,
        summary = "Validate a model content",
        description = "Allows to validate the model content without save it."
    )
    @PostMapping(path = "/models/{modelId}/validate", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(NO_CONTENT)
    void validateModel(
        @Parameter(description = VALIDATE_MODEL_ID_PARAM_DESCR, required = true) @PathVariable String modelId,
        @Parameter(description = VALIDATE_MODEL_FILE_PARAM_DESCR) @RequestPart(
            UPLOAD_FILE_PARAM_NAME
        ) MultipartFile file,
        @Parameter(description = VALIDATE_PROJECT_ID_PARAM_DESCR, required = false) @RequestParam(
            value = PROJECT_ID_PARAM_NAME,
            required = false
        ) String projectId,
        @Parameter(description = MODEL_USED_PARAM_DESCR, required = false) @RequestParam(
            value = MODEL_USED_PARAM_DESCR,
            required = false
        ) boolean isUsed
    ) throws IOException;

    @Operation(
        tags = MODELS,
        summary = "Validate model extensions",
        description = "Allows to validate the model extensions without save them."
    )
    @PostMapping(path = "/models/{modelId}/validate/extensions", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(NO_CONTENT)
    void validateModelExtensions(
        @Parameter(description = VALIDATE_MODEL_ID_PARAM_DESCR) @PathVariable String modelId,
        @Parameter(description = VALIDATE_EXTENSIONS_FILE_PARAM_DESCR) @RequestPart(
            UPLOAD_FILE_PARAM_NAME
        ) MultipartFile file,
        @Parameter(description = VALIDATE_PROJECT_ID_PARAM_DESCR, required = false) @RequestParam(
            value = PROJECT_ID_PARAM_NAME,
            required = false
        ) String projectId
    ) throws IOException;

    @Operation(
        tags = MODELS,
        summary = "List all the models that are not coupled to a project",
        description = "Get the models that has GLOBAL as scope. Minimal information for each model is returned."
    )
    @GetMapping(path = "/models")
    PagedModel<EntityModel<Model>> getGlobalModels(
        @Parameter(description = GET_MODELS_TYPE_PARAM_DESCR) @RequestParam(MODEL_TYPE_PARAM_NAME) String type,
        @Parameter(description = INCLUDE_ORPHANS_PARAM_DESCR, required = false) @RequestParam(
            value = INCLUDE_ORPHANS_PARAM_NAME,
            required = false,
            defaultValue = "false"
        ) boolean includeOrphans,
        Pageable pageable
    );

    @Operation(
        tags = MODELS,
        summary = "Add or update the relationship between an existing model, and the project",
        description = "Get the model associated with the project updated. Minimal information for the model is returned."
    )
    @PutMapping(path = "/projects/{projectId}/models/{modelId}")
    EntityModel<Model> putProjectModelRelationship(
        @Parameter(
            description = RELATE_MODEL_PROJECT_PROJECT_ID_PARAM_DESCR,
            required = true
        ) @PathVariable String projectId,
        @Parameter(
            description = RELATE_MODEL_PROJECT_MODEL_ID_PARAM_DESCR,
            required = true
        ) @PathVariable String modelId,
        @Parameter(description = SCOPE_PARAM_DESCR, required = false) @RequestParam(
            value = SCOPE_PARAM_NAME,
            required = false
        ) String scope,
        @Parameter(description = FORCE_PARAM_DESCR, required = false) @RequestParam(
            value = FORCE_PARAM_NAME,
            required = false,
            defaultValue = "false"
        ) boolean force
    );

    @Operation(
        tags = MODELS,
        summary = "Delete the relationship between an existing model, and the project",
        description = "Get the model associated with the project updated. " +
        "Minimal information for the model is returned."
    )
    @DeleteMapping(path = "/projects/{projectId}/models/{modelId}")
    EntityModel<Model> deleteProjectModelRelationship(
        @Parameter(
            description = DELETE_RELATIONSHIP_MODEL_PROJECT_PROJECT_ID_PARAM_DESCR,
            required = true
        ) @PathVariable String projectId,
        @Parameter(
            description = DELETE_RELATIONSHIP_MODEL_PROJECT_MODEL_ID_PARAM_DESCR,
            required = true
        ) @PathVariable String modelId
    );

    @Operation(
        tags = MODELS,
        summary = "Create new model that does note belong to a project",
        description = "Create a new model with no relationship to other projects"
    )
    @PostMapping(path = "/models")
    @ResponseStatus(CREATED)
    EntityModel<Model> createModelWithoutProject(
        @Parameter(description = CREATE_MODEL_PARAM_DESCR) @RequestBody Model model
    );
}
