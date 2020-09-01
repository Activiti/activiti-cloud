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

import java.util.List;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.springframework.http.ResponseEntity;

public interface ProcessVariablesRuntimeAdminService {

    @RequestLine("PUT /admin/v1/process-instances/{id}/variables")
    @Headers("Content-Type: application/json")
    ResponseEntity<List<String>> updateVariables(@Param("id") String id,
                                                 SetProcessVariablesPayload setProcessVariablesPayload);
    
    @RequestLine("DELETE /admin/v1/process-instances/{id}/variables")
    @Headers("Content-Type: application/json")
    ResponseEntity<Void> removeVariables(@Param("id") String id,
                                         RemoveProcessVariablesPayload removeProcessVariablesPayload);

}
