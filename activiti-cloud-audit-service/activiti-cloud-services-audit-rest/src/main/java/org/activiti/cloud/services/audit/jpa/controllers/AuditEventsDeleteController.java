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

import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.audit.api.resources.EventsLinkRelationProvider;
import org.activiti.cloud.services.audit.jpa.AuditFeatureToggles;
import org.activiti.cloud.services.audit.jpa.model.AuditEventsDeletionCancelResponse;
import org.activiti.cloud.services.audit.jpa.model.AuditEventsDeletionStartResponse;
import org.activiti.cloud.services.audit.jpa.model.AuditEventsDeletionStatusResponse;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsDeletionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "activiti.rest.enable-deletion", matchIfMissing = true)
@RestController
@RequestMapping(
    value = "/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL,
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public class AuditEventsDeleteController {

    private final AuditEventsDeletionService auditEventsDeletionService;
    private final FeatureToggle featureToggle;

    @Autowired
    public AuditEventsDeleteController(AuditEventsDeletionService auditEventsDeletionService, FeatureToggle featureToggle) {
        this.auditEventsDeletionService = auditEventsDeletionService;
        this.featureToggle = featureToggle;
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<AuditEventsDeletionStartResponse> deleteEvents() {
        if (!featureToggle.isEnabled(AuditFeatureToggles.AUDIT_CANCELLABLE_DELETE)) {
            return ResponseEntity.notFound().build();
        }

        if (!auditEventsDeletionService.startDeletion()) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                    new AuditEventsDeletionStartResponse(
                        "Audit events deletion is already running",
                        auditEventsDeletionService.getStatusResponse()
                    )
                );
        }

        auditEventsDeletionService.deleteEventsAsync();

        return ResponseEntity
            .accepted()
            .body(
                new AuditEventsDeletionStartResponse(
                    "Audit events deletion started",
                    auditEventsDeletionService.getStatusResponse()
                )
            );
    }

    @RequestMapping(value = "/deletion/cancel", method = RequestMethod.POST)
    public ResponseEntity<AuditEventsDeletionCancelResponse> cancelDeletion() {
        if (!featureToggle.isEnabled(AuditFeatureToggles.AUDIT_CANCELLABLE_DELETE)) {
            return ResponseEntity.notFound().build();
        }

        if (!auditEventsDeletionService.requestCancellation()) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                    new AuditEventsDeletionCancelResponse(
                        "No audit events deletion is currently running",
                        auditEventsDeletionService.getStatusResponse()
                    )
                );
        }

        return ResponseEntity
            .accepted()
            .body(
                new AuditEventsDeletionCancelResponse(
                    "Audit events deletion cancellation requested",
                    auditEventsDeletionService.getStatusResponse()
                )
            );
    }

    @RequestMapping(value = "/deletion/status", method = RequestMethod.GET)
    public ResponseEntity<AuditEventsDeletionStatusResponse> getDeletionStatus() {
        if (!featureToggle.isEnabled(AuditFeatureToggles.AUDIT_CANCELLABLE_DELETE)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(auditEventsDeletionService.getStatusResponse());
    }
}
