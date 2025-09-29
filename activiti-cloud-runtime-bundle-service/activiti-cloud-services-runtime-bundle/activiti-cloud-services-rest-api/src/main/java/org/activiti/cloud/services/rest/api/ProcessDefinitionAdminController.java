/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
import java.util.List;
import java.util.Set;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.activiti.spring.process.model.ProcessVariableDefinition;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public interface ProcessDefinitionAdminController {
    @GetMapping(
        value = "/admin/v1/process-definitions",
        produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    PagedModel<EntityModel<ExtendedCloudProcessDefinition>> getAllProcessDefinitions(
        @Parameter(description = "List of values to include in response") @RequestParam(
            value = "include",
            required = false
        ) List<String> include,
        @Parameter(
            description = "Specifies whether to include latest versions only (true) or all the versions (false) of each process definition"
        ) @RequestParam(value = "latestVersion", required = false, defaultValue = "false") boolean latestVersion,
        @Parameter(description = "Process definition category to exclude from results") @RequestParam(
            value = "excludedCategory",
            required = false
        ) String excludedCategory,
        Pageable pageable
    );

    @GetMapping(value = "/admin/v1/process-variable-definitions", produces = "application/json")
    @ResponseBody
    Set<ProcessVariableDefinition> getProcessVariableDefinitions();
}
