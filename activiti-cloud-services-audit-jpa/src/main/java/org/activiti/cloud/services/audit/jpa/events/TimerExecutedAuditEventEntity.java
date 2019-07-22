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

import org.activiti.cloud.api.process.model.events.CloudBPMNTimerExecutedEvent;

@Entity
@DiscriminatorValue(value = TimerExecutedAuditEventEntity.TIMER_EXECUTED_EVENT)
public class TimerExecutedAuditEventEntity extends TimerAuditEventEntity {

    protected static final String TIMER_EXECUTED_EVENT = "TimerExecutedEvent";
    
    public TimerExecutedAuditEventEntity() {
    }

    public TimerExecutedAuditEventEntity(CloudBPMNTimerExecutedEvent cloudEvent) {
        super(cloudEvent);
    }   
}
