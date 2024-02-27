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
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface ProcessInstanceTasksController {
    @GetMapping(value = "/v1/process-instances/{processInstanceId}/tasks")
    @CollectionFormat(feign.CollectionFormat.CSV)
    PagedModel<EntityModel<CloudTask>> getTasks(
        @Parameter(description = "Enter the processInstanceId to get tasks") @PathVariable(
            value = "processInstanceId"
        ) String processInstanceId,
        Pageable pageable
    );
}
