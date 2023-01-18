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
package org.activiti.cloud.acc.core.services.runtime.diagram;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Runtime Bundle service to manage diagrams
 */
public interface ProcessRuntimeDiagramService {

    @RequestLine("GET /v1/process-instances/{id}/model")
    @Headers("Content-Type: image/svg+xml")
    String getProcessInstanceModel(@Param("id") String id);

    @RequestLine("GET /v1/process-definitions/{id}/model")
    @Headers("Accept: image/svg+xml")
    String getProcessDefinitionModel(@Param("id") String id);

}
