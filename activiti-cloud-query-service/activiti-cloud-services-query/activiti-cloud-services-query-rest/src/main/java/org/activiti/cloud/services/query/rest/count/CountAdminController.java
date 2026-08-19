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
package org.activiti.cloud.services.query.rest.count;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.activiti.cloud.services.query.rest.payload.BatchCountRequest;
import org.activiti.cloud.services.query.rest.payload.ResourceType;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/admin/v1/count", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
public class CountAdminController {

    private final CountService countService;

    public CountAdminController(CountService countService) {
        this.countService = countService;
    }

    @Operation(
        summary = "Count resources for multiple filters and resource types in a single request (admin)",
        description = "Body is keyed by resource type (TASK, PROCESS_INSTANCE) with a list of search requests, each " +
            "of which must specify a unique requestId. Returns the count for each filter keyed by its requestId without " +
            "user restriction."
    )
    @PostMapping
    public Map<ResourceType, Map<String, Long>> count(@RequestBody BatchCountRequest request) {
        return countService.countUnrestricted(request);
    }
}
