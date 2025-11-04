/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.events.handlers;

import jakarta.persistence.EntityManager;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BaseBPMNActivityEntity;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;

public abstract class BaseBPMNActivityEventHandler {

    protected final EntityManager entityManager;

    public BaseBPMNActivityEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Retrieves the IntegrationContextEntity associated with a BPMNActivityEntity.
     * Since IntegrationContext has a one-to-one relationship with ServiceTask,
     * this method will only return a value for service task activities.
     *
     * @param bpmnActivityEntity the BPMN activity entity
     * @return the associated IntegrationContextEntity, or null if not found or not a service task
     */
    protected IntegrationContextEntity getIntegrationContext(BaseBPMNActivityEntity bpmnActivityEntity) {
        if (bpmnActivityEntity == null) {
            return null;
        }

        // Only service tasks have integration context
        if (bpmnActivityEntity instanceof ServiceTaskEntity) {
            ServiceTaskEntity serviceTaskEntity = (ServiceTaskEntity) bpmnActivityEntity;
            return serviceTaskEntity.getIntegrationContext();
        }

        return null;
    }

    /**
     * Retrieves the IntegrationContextEntity associated with a BPMNActivity.
     * Since IntegrationContext has a one-to-one relationship with ServiceTask,
     * this method will only return a value for service task activities.
     *
     * @param bpmnActivity the BPMN activity domain model
     * @return the associated IntegrationContextEntity, or null if not found or not a service task
     */
    protected IntegrationContextEntity getIntegrationContext(BPMNActivity bpmnActivity) {
        if (bpmnActivity == null || !"serviceTask".equals(bpmnActivity.getActivityType())) {
            return null;
        }

        // Build the primary key for the service task
        String pkId = BPMNActivityEntity.IdBuilderHelper.from(bpmnActivity);

        // Find the service task entity
        ServiceTaskEntity serviceTaskEntity = entityManager.find(ServiceTaskEntity.class, pkId);

        if (serviceTaskEntity != null) {
            return serviceTaskEntity.getIntegrationContext();
        }

        return null;
    }

    protected BaseBPMNActivityEntity findOrCreateBPMNActivityEntity(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        String pkId;

        BaseBPMNActivityEntity bpmnActivityEntity = null;

        if ("serviceTask".equals(bpmnActivity.getActivityType())) {
            pkId = IntegrationContextEntity.IdBuilderHelper.from(getIntegrationContext(bpmnActivity));
            bpmnActivityEntity = entityManager.find(ServiceTaskEntity.class, pkId);
        } else {
            pkId = BPMNActivityEntity.IdBuilderHelper.from(bpmnActivity);
            bpmnActivityEntity = entityManager.find(BPMNActivityEntity.class, pkId);
        }

        if (bpmnActivityEntity == null) {
            bpmnActivityEntity = createBpmnActivityEntity(event, pkId);
        }

        return bpmnActivityEntity;
    }

    public BaseBPMNActivityEntity createBpmnActivityEntity(CloudRuntimeEvent<?, ?> event, String pkId) {
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        BaseBPMNActivityEntity bpmnActivityEntity;

        if ("serviceTask".equals(bpmnActivity.getActivityType())) {
            bpmnActivityEntity =
                new ServiceTaskEntity(
                    event.getServiceName(),
                    event.getServiceFullName(),
                    event.getServiceVersion(),
                    event.getAppName(),
                    event.getAppVersion()
                );
        } else {
            bpmnActivityEntity =
                new BPMNActivityEntity(
                    event.getServiceName(),
                    event.getServiceFullName(),
                    event.getServiceVersion(),
                    event.getAppName(),
                    event.getAppVersion()
                );
        }

        bpmnActivityEntity.setId(pkId);
        bpmnActivityEntity.setElementId(bpmnActivity.getElementId());
        bpmnActivityEntity.setActivityName(bpmnActivity.getActivityName());
        bpmnActivityEntity.setActivityType(bpmnActivity.getActivityType());
        bpmnActivityEntity.setProcessDefinitionId(bpmnActivity.getProcessDefinitionId());
        bpmnActivityEntity.setProcessInstanceId(bpmnActivity.getProcessInstanceId());
        bpmnActivityEntity.setExecutionId(bpmnActivity.getExecutionId());
        bpmnActivityEntity.setProcessDefinitionKey(activityEvent.getProcessDefinitionKey());
        bpmnActivityEntity.setProcessDefinitionVersion(activityEvent.getProcessDefinitionVersion());
        bpmnActivityEntity.setBusinessKey(activityEvent.getBusinessKey());

        return bpmnActivityEntity;
    }
}
