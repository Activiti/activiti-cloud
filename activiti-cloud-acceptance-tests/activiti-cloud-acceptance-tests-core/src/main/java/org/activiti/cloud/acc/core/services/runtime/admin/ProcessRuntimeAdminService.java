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
package org.activiti.cloud.acc.core.services.runtime.admin;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.hateoas.PagedModel;

public interface ProcessRuntimeAdminService {

    @RequestLine("GET /admin/v1/process-instances?sort=startDate,desc&sort=id,desc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    @CollectionFormat(feign.CollectionFormat.CSV)
    PagedModel<CloudProcessInstance> getProcessInstances();

    @RequestLine("DELETE /admin/v1/process-instances/{id}")
    void deleteProcess(@Param("id") String id);

    @RequestLine("POST /admin/v1/process-instances/message")
    @Headers("Content-Type: application/json")
    CloudProcessInstance message(StartMessagePayload startProcess);

    @RequestLine("PUT /admin/v1/process-instances/message")
    @Headers("Content-Type: application/json")
    void message(ReceiveMessagePayload startProcess);

}
