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

import org.activiti.cloud.api.process.model.events.CloudProcessResumedEvent;

@Entity
@DiscriminatorValue(value = ProcessResumedAuditEventEntity.PROCESS_RESUMED_EVENT)
public class ProcessResumedAuditEventEntity extends ProcessAuditEventEntity {

    protected static final String PROCESS_RESUMED_EVENT = "ProcessResumedEvent";

    public ProcessResumedAuditEventEntity() {
    }

    public ProcessResumedAuditEventEntity(CloudProcessResumedEvent cloudEvent) {
        super(cloudEvent);
    }
}
