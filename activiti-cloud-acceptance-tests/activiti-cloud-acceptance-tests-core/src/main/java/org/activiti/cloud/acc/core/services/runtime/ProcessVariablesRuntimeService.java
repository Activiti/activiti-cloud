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
package org.activiti.cloud.acc.core.services.runtime;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;

public interface ProcessVariablesRuntimeService {

    @RequestLine("GET /v1/process-instances/{id}/variables")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    CollectionModel<CloudVariableInstance> getVariables(@Param("id") String id);

    @RequestLine("PUT /v1/process-instances/{id}/variables")
    @Headers("Content-Type: application/json")
    ResponseEntity<Void> setVariables(@Param("id") String id,
                                      SetProcessVariablesPayload setProcessVariablesPayload);

}
