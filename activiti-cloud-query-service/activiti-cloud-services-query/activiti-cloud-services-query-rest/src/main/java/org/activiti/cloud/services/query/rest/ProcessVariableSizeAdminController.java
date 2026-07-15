/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.rest.dto.ProcessVariableSizeInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/admin/v1/process-variables/size", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Process Variables Size Admin", description = "Operational dashboard endpoints for detecting large process variables")
public class ProcessVariableSizeAdminController {

    private static final int DEFAULT_MIN_SIZE = 4000;
    private static final String DEFAULT_MIN_SIZE_VALUE = "4000";

    private final VariableRepository variableRepository;

    public ProcessVariableSizeAdminController(VariableRepository variableRepository) {
        this.variableRepository = variableRepository;
    }

    @GetMapping
    @Operation(
        summary = "Find large process variables",
        description = "Returns process variables whose serialized value size exceeds the given threshold, ordered by size descending. " +
            "Useful for operational monitoring and detecting variables that may impact performance."
    )
    public Page<ProcessVariableSizeInfo> findLargeVariables(
        @Parameter(description = "Minimum variable value size in characters (default: 4000)")
        @RequestParam(name = "minSize", defaultValue = DEFAULT_MIN_SIZE_VALUE) int minSize,
        Pageable pageable
    ) {
        return variableRepository.findLargeVariables(minSize, pageable).map(this::toDto);
    }

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_PROCESS_INSTANCE_ID = 3;
    private static final int COL_PROCESS_DEFINITION_KEY = 4;
    private static final int COL_VALUE_SIZE = 5;

    private ProcessVariableSizeInfo toDto(Object[] row) {
        return new ProcessVariableSizeInfo(
            ((Number) row[COL_ID]).longValue(),
            (String) row[COL_NAME],
            (String) row[COL_TYPE],
            (String) row[COL_PROCESS_INSTANCE_ID],
            (String) row[COL_PROCESS_DEFINITION_KEY],
            ((Number) row[COL_VALUE_SIZE]).longValue()
        );
    }
}
