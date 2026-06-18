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
package org.activiti.cloud.services.query.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import org.activiti.cloud.api.process.model.CloudBPMNActivity.BPMNActivityStatus;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Slim, view-oriented projection of a BPMN activity used to render the
 * execution path on a process definition diagram. Only fields needed by the
 * front-end diagram renderer are exposed; service / process metadata that is
 * already implied by the request URL is intentionally omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BpmnDiagramActivityEntry {

    private final String id;
    private final String elementId;
    private final String activityType;
    private final BPMNActivityStatus status;
    private final String executionId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private final Date startedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private final Date completedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private final Date cancelledDate;

    public BpmnDiagramActivityEntry(
        String id,
        String elementId,
        String activityType,
        BPMNActivityStatus status,
        String executionId,
        Date startedDate,
        Date completedDate,
        Date cancelledDate
    ) {
        this.id = id;
        this.elementId = elementId;
        this.activityType = activityType;
        this.status = status;
        this.executionId = executionId;
        this.startedDate = startedDate;
        this.completedDate = completedDate;
        this.cancelledDate = cancelledDate;
    }

    public String getId() {
        return id;
    }

    public String getElementId() {
        return elementId;
    }

    public String getActivityType() {
        return activityType;
    }

    public BPMNActivityStatus getStatus() {
        return status;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Date getStartedDate() {
        return startedDate;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public Date getCancelledDate() {
        return cancelledDate;
    }
}
