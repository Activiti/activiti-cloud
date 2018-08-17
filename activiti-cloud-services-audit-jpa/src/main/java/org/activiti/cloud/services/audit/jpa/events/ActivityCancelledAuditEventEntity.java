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

package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.BPMNActivity;

@Entity
@DiscriminatorValue(value = ActivityCancelledAuditEventEntity.ACTIVITY_CANCELLED_EVENT)
public class ActivityCancelledAuditEventEntity extends BPMNActivityAuditEventEntity {

    protected static final String ACTIVITY_CANCELLED_EVENT = "ActivityCancelledEvent";

    private String cause;

    public ActivityCancelledAuditEventEntity() {
    }

    public ActivityCancelledAuditEventEntity(String eventId,
                                             Long timestamp,
                                             String cause) {
        super(eventId,
              timestamp,
              BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name());
        this.cause = cause;
    }

    public ActivityCancelledAuditEventEntity(String eventId,
                                             Long timestamp,
                                             String appName,
                                             String appVersion,
                                             String serviceName,
                                             String serviceFullName,
                                             String serviceType,
                                             String serviceVersion,
                                             BPMNActivity bpmnActivity,
                                             String cause) {
        super(eventId,
              timestamp,
              BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name(),
              appName,
              appVersion,
              serviceName,
              serviceFullName,
              serviceType,
              serviceVersion,
              bpmnActivity);
        this.cause = cause;
    }

    public String getCause() {
        return cause;
    }
}
