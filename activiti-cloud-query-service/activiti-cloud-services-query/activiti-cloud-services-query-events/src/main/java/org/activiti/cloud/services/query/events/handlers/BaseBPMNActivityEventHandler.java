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
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BaseBPMNActivityEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;

public abstract class BaseBPMNActivityEventHandler {

    protected final EntityManager entityManager;

    public BaseBPMNActivityEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected BaseBPMNActivityEntity findOrCreateBPMNActivityEntity(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityEvent activityEvent = (CloudBPMNActivityEvent) event;

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        // Use the ID directly from CloudBPMNActivity if available, otherwise fall back to composite ID
        String activityId;
        if (bpmnActivity instanceof CloudBPMNActivity && ((CloudBPMNActivity) bpmnActivity).getId() != null) {
            activityId = ((CloudBPMNActivity) bpmnActivity).getId();
        } else {
            activityId = BPMNActivityEntity.IdBuilderHelper.from(bpmnActivity);
        }

        BaseBPMNActivityEntity bpmnActivityEntity;

        if ("serviceTask".equals(bpmnActivity.getActivityType())) {
            bpmnActivityEntity = entityManager.find(ServiceTaskEntity.class, activityId);
        } else {
            bpmnActivityEntity = entityManager.find(BPMNActivityEntity.class, activityId);
        }

        if (bpmnActivityEntity == null) {
            bpmnActivityEntity = createBpmnActivityEntity(event, activityId);
        }

        return bpmnActivityEntity;
    }

    public BaseBPMNActivityEntity createBpmnActivityEntity(CloudRuntimeEvent<?, ?> event, String activityId) {
        CloudBPMNActivityEvent activityEvent = (CloudBPMNActivityEvent) event;

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

        bpmnActivityEntity.setId(activityId);
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
