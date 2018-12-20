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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Lob;

import org.activiti.api.process.model.BPMNActivity;
import org.activiti.cloud.services.audit.jpa.converters.json.ActivityJpaJsonConverter;

@Entity
public abstract class BPMNActivityAuditEventEntity extends AuditEventEntity {

    @Convert(converter = ActivityJpaJsonConverter.class)
    @Lob
    @Column
    private BPMNActivity bpmnActivity;


    public BPMNActivityAuditEventEntity() {
    }

    public BPMNActivityAuditEventEntity(String eventId,
                                        Long timestamp,
                                        String eventType) {
        super(eventId,
              timestamp,
              eventType);
    }

    public BPMNActivityAuditEventEntity(String eventId,
                                        Long timestamp,
                                        String eventType,
                                        String appName,
                                        String appVersion,
                                        String serviceName,
                                        String serviceFullName,
                                        String serviceType,
                                        String serviceVersion,
                                        BPMNActivity bpmnActivity) {
        super(eventId,
              timestamp,
              eventType);
        setAppName(appName);
        setAppVersion(appVersion);
        setServiceName(serviceName);
        setServiceFullName(serviceFullName);
        setServiceType(serviceType);
        setServiceVersion(serviceVersion);
        setBpmnActivity(bpmnActivity);
    }

    public BPMNActivity getBpmnActivity() {
        return bpmnActivity;
    }

    public void setBpmnActivity(BPMNActivity bpmnActivity) {
        this.bpmnActivity = bpmnActivity;
    }

}
