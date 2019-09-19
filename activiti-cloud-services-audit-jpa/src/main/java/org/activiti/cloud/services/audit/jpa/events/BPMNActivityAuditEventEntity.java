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

import org.activiti.api.process.model.BPMNActivity;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.ActivityJpaJsonConverter;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BPMNActivityAuditEventEntity extends AuditEventEntity {

    @Convert(converter = ActivityJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private BPMNActivity bpmnActivity;

    public BPMNActivityAuditEventEntity() {
    }

    public BPMNActivityAuditEventEntity(CloudBPMNActivityEvent cloudEvent) {
        super(cloudEvent);
        setBpmnActivity(cloudEvent.getEntity());
    }
    

    public BPMNActivity getBpmnActivity() {
        return bpmnActivity;
    }

    public void setBpmnActivity(BPMNActivity bpmnActivity) {
        this.bpmnActivity = bpmnActivity;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(bpmnActivity);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BPMNActivityAuditEventEntity other = (BPMNActivityAuditEventEntity) obj;
        return Objects.equals(bpmnActivity, other.bpmnActivity);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BPMNActivityAuditEventEntity [bpmnActivity=")
               .append(bpmnActivity)
               .append(", toString()=")
               .append(super.toString())
               .append("]");
        return builder.toString();
    }

}
