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
package org.activiti.cloud.acc.core.services.audit;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.springframework.hateoas.PagedModel;

public interface AuditService extends BaseService {

    @RequestLine("GET /v1/events?search={search}&sort=timestamp,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedModel<CloudRuntimeEvent> getEvents(@Param("search") String search);

    @RequestLine("GET /v1/events?sort=timestamp,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedModel<CloudRuntimeEvent> getEvents();
}
