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
package org.activiti.cloud.services.audit.jpa.controllers.v2;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.controllers.AuditEventsController;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.api.resources.EventsLinkRelationProvider;
import org.activiti.cloud.services.audit.api.search.SearchParams;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/v2/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL,
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public class AuditEventsControllerV2Impl implements AuditEventsController {

    private final AuditEventsService auditEventsService;

    @Autowired
    public AuditEventsControllerV2Impl(AuditEventsService auditEventsService) {
        this.auditEventsService = auditEventsService;
    }

    @RequestMapping(value = "/{eventId}", method = RequestMethod.GET)
    public EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>> findById(@PathVariable String eventId) {
        return auditEventsService.findEventById(eventId);
    }

    /**
     * Searches audit events using slice-based pagination.
     * <p>
     * Unlike the v1 endpoint, this version fetches results as a {@link org.springframework.data.domain.Slice}
     * rather than a full {@link org.springframework.data.domain.Page}. It therefore avoids the expensive
     * {@code COUNT} query: instead of an exact total, it fetches one extra element to determine whether a
     * next page exists and reports an estimated element count derived from the current offset. This trades
     * an exact {@code totalElements} value for better performance on large audit datasets.
     */
    @RequestMapping(method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>>> search(
        SearchParams searchParams,
        Pageable pageable
    ) {
        return auditEventsService.searchEventsSliced(searchParams, pageable);
    }
}
