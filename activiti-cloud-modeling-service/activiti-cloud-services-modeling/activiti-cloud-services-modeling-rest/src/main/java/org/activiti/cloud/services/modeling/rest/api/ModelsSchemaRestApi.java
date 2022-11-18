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

import static org.activiti.cloud.services.modeling.rest.api.ModelRestApi.MODELS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Schema resources.
 */
@RestController
@Tag(name = MODELS)
@RequestMapping(value = "/v1/schemas")
public interface ModelsSchemaRestApi {
    @Operation(
        tags = MODELS,
        summary = "Get validation schema for model type",
        description = "Get the content of the schema used to validate the given model type."
    )
    @GetMapping(value = "/{modelType}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> getSchema(@PathVariable String modelType);
}
