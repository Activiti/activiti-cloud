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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Tag(name = "Process Definition Controller")
public interface ProcessDefinitionController {
    @GetMapping("/v1/process-definitions")
    @CollectionFormat(feign.CollectionFormat.CSV)
    PagedModel<EntityModel<ExtendedCloudProcessDefinition>> getProcessDefinitions(
        @Parameter(description = "List of values to include in response") @RequestParam(
            value = "include",
            required = false
        ) List<String> include,
        Pageable pageable
    );

    @GetMapping(value = "/v1/process-definitions/{id}")
    EntityModel<CloudProcessDefinition> getProcessDefinition(
        @Parameter(description = "Enter the id to get process definition") @PathVariable(value = "id") String id
    );

    @GetMapping(value = "/v1/process-definitions/{id}/model", produces = "application/xml")
    @ResponseBody
    String getProcessModel(
        @Parameter(description = "Enter the id to get process model") @PathVariable(value = "id") String id
    );

    @GetMapping(value = "/v1/process-definitions/{id}/model", produces = "application/json")
    @ResponseBody
    String getBpmnModel(
        @Parameter(description = "Enter the id to get Bpmn model") @PathVariable(value = "id") String id
    );

    @GetMapping(value = "/v1/process-definitions/{id}/model", produces = "image/svg+xml")
    @ResponseBody
    String getProcessDiagram(
        @Parameter(description = "Enter the id to get process diagram") @PathVariable(value = "id") String id
    );

    @GetMapping(value = "/v1/process-definitions/{id}/static-values", produces = "application/json")
    @ResponseBody
    Map<String, Object> getProcessModelStaticValuesMappingForStartEvent(
        @Parameter(
            description = "Enter the id to get process model static values mapping for start event"
        ) @PathVariable(value = "id") String id
    );
}
