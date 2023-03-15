/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.query.model;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity(name = "BPMNActivity")
@Table(
    name = "BPMN_ACTIVITY",
    indexes = {
        @Index(name = "bpmn_activity_status_idx", columnList = "status", unique = false),
        @Index(name = "bpmn_activity_processInstance_idx", columnList = "processInstanceId", unique = false),
        @Index(
            name = "bpmn_activity_processInstance_elementId_idx",
            columnList = "processInstanceId,elementId,executionId",
            unique = true
        ),
    }
)
@DynamicInsert
@DynamicUpdate
public class BPMNActivityEntity extends BaseBPMNActivityEntity implements CloudBPMNActivity {

    public BPMNActivityEntity() {}

    public BPMNActivityEntity(
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion
    ) {
        super(serviceName, serviceFullName, serviceVersion, appName, appVersion);
    }

    public static class IdBuilderHelper {

        public static String from(BPMNActivity bpmnActivity) {
            return new StringBuilder()
                .append(bpmnActivity.getProcessInstanceId())
                .append(":")
                .append(bpmnActivity.getElementId())
                .append(":")
                .append(bpmnActivity.getExecutionId())
                .toString();
        }
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        BPMNActivityEntity other = (BPMNActivityEntity) obj;

        return getId() != null && Objects.equals(getId(), other.getId());
    }
}
