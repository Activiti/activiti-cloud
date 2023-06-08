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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import org.activiti.cloud.api.process.model.CloudServiceTask;
import org.hibernate.annotations.*;

@Entity(name = "ServiceTask")
@Table(name = "BPMN_ACTIVITY")
@Where(clause = "activity_type='serviceTask'")
@DynamicInsert
@DynamicUpdate
public class ServiceTaskEntity extends BaseBPMNActivityEntity implements CloudServiceTask {

    @JsonIgnore
    @OneToOne(mappedBy = "serviceTask", fetch = FetchType.LAZY, optional = true)
    private IntegrationContextEntity integrationContext;

    protected ServiceTaskEntity() {}

    public ServiceTaskEntity(
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion
    ) {
        super(serviceName, serviceFullName, serviceVersion, appName, appVersion);
    }

    public IntegrationContextEntity getIntegrationContext() {
        return integrationContext;
    }

    public void setIntegrationContext(IntegrationContextEntity integrationContext) {
        this.integrationContext = integrationContext;
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
        ServiceTaskEntity other = (ServiceTaskEntity) obj;

        return this.getId() != null && Objects.equals(this.getId(), other.getId());
    }
}
