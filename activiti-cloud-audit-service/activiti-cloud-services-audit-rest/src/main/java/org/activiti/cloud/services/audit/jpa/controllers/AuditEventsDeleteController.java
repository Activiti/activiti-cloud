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
package org.activiti.cloud.services.audit.jpa.controllers;

import java.util.Map;
import org.activiti.cloud.services.audit.api.resources.EventsLinkRelationProvider;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsDeleteService;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsDeleteService.DeleteStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@ConditionalOnProperty(name = "activiti.rest.enable-deletion", matchIfMissing = true)
@RestController
@RequestMapping(
    value = "/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL,
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public class AuditEventsDeleteController {

    private final AuditEventsDeleteService deleteService;

    @Autowired
    public AuditEventsDeleteController(AuditEventsDeleteService deleteService) {
        this.deleteService = deleteService;
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteEvents() {
        try {
            deleteService.startDeletion();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        return ResponseEntity.accepted().body(buildStatusResponse());
    }

    @RequestMapping(value = "/delete/stop", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> stopDeleteEvents() {
        try {
            deleteService.stopDeletion();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        return ResponseEntity.ok(buildStatusResponse());
    }

    @RequestMapping(value = "/delete/status", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getDeleteStatus() {
        return ResponseEntity.ok(buildStatusResponse());
    }

    private Map<String, Object> buildStatusResponse() {
        DeleteStatus status = deleteService.getStatus();
        return Map.of(
            "status", status.name(),
            "deletedCount", deleteService.getDeletedCount(),
            "totalCount", deleteService.getTotalCount()
        );
    }
}
