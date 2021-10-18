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
package org.activiti.cloud.services.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping(value = "/v1/process-definitions",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessDefinitionController {

    @GetMapping()
    PagedModel<EntityModel<CloudProcessDefinition>> getProcessDefinitions(Pageable pageable);


    @GetMapping(value = "/{id}")
    EntityModel<CloudProcessDefinition> getProcessDefinition(@PathVariable(value = "id") String id);

    @GetMapping(value = "/{id}/model",
            produces = "application/xml")
    @ResponseBody
    @Operation(summary = "getProcessModel")
    String getProcessModel(@PathVariable(value = "id") String id);

    @GetMapping(value = "/{id}/model",
            produces = "application/json")
    @ResponseBody
    @Operation(summary = "getProcessModel")
    String getBpmnModel(@PathVariable(value = "id") String id);

    @GetMapping(value = "/{id}/model",
            produces = "image/svg+xml")
    @ResponseBody
    @Operation(summary = "getProcessModel")
    String getProcessDiagram(@PathVariable(value = "id") String id);
}
