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
package org.activiti.cloud.services.query.model;

import com.querydsl.core.annotations.PropertyType;
import com.querydsl.core.annotations.QueryType;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.activiti.cloud.api.process.model.CloudServiceTask;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Where;
import org.springframework.format.annotation.DateTimeFormat;

@Entity(name = "ServiceTask")
@Table(name = "BPMN_ACTIVITY")
@Where(clause = "activity_type='serviceTask'")
@DynamicInsert
@DynamicUpdate
public class ServiceTaskEntity extends BaseBPMNActivityEntity implements CloudServiceTask {

    @OneToMany(mappedBy = "serviceTask", fetch = FetchType.LAZY)
    private List<IntegrationContextEntity> integrationContexts;

    @Column(name = "integration_context_counter")
    private Integer integrationContextCounter = 0;

    @QueryType(PropertyType.DATE)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Transient
    private Date startedFrom;

    @QueryType(PropertyType.DATE)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Transient
    private Date startedTo;

    @QueryType(PropertyType.DATE)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Transient
    @Access(AccessType.PROPERTY)
    private Date completedFrom;

    @QueryType(PropertyType.DATE)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Transient
    private Date completedTo;

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

    public List<IntegrationContextEntity> getIntegrationContexts() {
        return integrationContexts;
    }

    public void setIntegrationContexts(List<IntegrationContextEntity> integrationContexts) {
        this.integrationContexts = integrationContexts;
    }

    @Override
    public Integer getIntegrationContextCounter() {
        return integrationContextCounter != null ? integrationContextCounter : 0;
    }

    public void setIntegrationContextCounter(Integer integrationContextCounter) {
        this.integrationContextCounter = integrationContextCounter;
    }

    /**
     * Increments the integration context counter by one.
     * Called when a new integration context is created.
     */
    public void incrementIntegrationContextCounter() {
        this.integrationContextCounter = getIntegrationContextCounter() + 1;
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
