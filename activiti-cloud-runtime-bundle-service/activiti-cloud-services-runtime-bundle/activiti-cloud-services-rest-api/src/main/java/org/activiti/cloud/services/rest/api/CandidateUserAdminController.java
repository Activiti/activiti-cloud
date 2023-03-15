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

import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(
    value = "/admin/v1/tasks/{taskId}/candidate-users",
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public interface CandidateUserAdminController {
    @RequestMapping(method = RequestMethod.POST)
    void addCandidateUsers(
        @PathVariable("taskId") String taskId,
        @RequestBody CandidateUsersPayload candidateUsersPayload
    );

    @RequestMapping(method = RequestMethod.DELETE)
    void deleteCandidateUsers(
        @PathVariable("taskId") String taskId,
        @RequestBody CandidateUsersPayload candidateUsersPayload
    );

    @RequestMapping(method = RequestMethod.GET)
    CollectionModel<EntityModel<CandidateUser>> getUserCandidates(@PathVariable("taskId") String taskId);
}
