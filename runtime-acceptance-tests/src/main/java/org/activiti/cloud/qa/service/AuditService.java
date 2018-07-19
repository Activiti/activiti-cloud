/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.service;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.springframework.hateoas.PagedResources;

/**
 * Audit service
 */
public interface AuditService extends BaseService {

    @RequestLine("GET /v1/events?search={search}")
    @Headers("Content-Type: application/json")
    PagedResources<CloudRuntimeEvent> getProcessInstanceEvents(@Param("search") String search);

    @RequestLine("GET /v1/events?sort=timestamp,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudRuntimeEvent> getEvents();

    @RequestLine("GET /v1/events?search={search}")
    @Headers("Content-Type: application/json")
    PagedResources<CloudRuntimeEvent> getEventsByEntityId(@Param("search") String search);
}
