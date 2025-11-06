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
import java.util.Optional;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BaseBPMNActivityEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseBPMNActivityEventHandler {

    Logger logger = LoggerFactory.getLogger(BaseBPMNActivityEventHandler.class);

    protected final EntityManager entityManager;

    public BaseBPMNActivityEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected Optional<BaseBPMNActivityEntity> findOrCreateBPMNActivityEntity(CloudRuntimeEvent<?, ?> event) {
        logger.error("AAE-39414: Handling CloudBPMNActivityEvent");
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        String pkId = BPMNActivityEntity.IdBuilderHelper.from(bpmnActivity);
        logger.error("AAE-39415: pkId " + pkId);

        BaseBPMNActivityEntity bpmnActivityEntity = null;

        if (!"serviceTask".equals(bpmnActivity.getActivityType())) {
            bpmnActivityEntity = entityManager.find(BPMNActivityEntity.class, pkId);
        }

        if (bpmnActivityEntity == null) {
            return createBpmnActivityEntity(event);
        } else {
            return Optional.of(bpmnActivityEntity);
        }
    }

    public Optional<BaseBPMNActivityEntity> createBpmnActivityEntity(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        String pkId = BPMNActivityEntity.IdBuilderHelper.from(bpmnActivity);
        logger.error("AAE-39417: Creating new BaseBPMNActivityEntity with pkId: " + pkId);

        BaseBPMNActivityEntity bpmnActivityEntity = null;

        if (!"serviceTask".equals(bpmnActivity.getActivityType())) {
            bpmnActivityEntity =
                new BPMNActivityEntity(
                    event.getServiceName(),
                    event.getServiceFullName(),
                    event.getServiceVersion(),
                    event.getAppName(),
                    event.getAppVersion()
                );
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
        } else {
            String serviceTaskInfo =
                "ServiceName: " +
                event.getServiceName() +
                ", ServiceFullName: " +
                event.getServiceFullName() +
                ", ServiceVersion: " +
                event.getServiceVersion() +
                ", AppName: " +
                event.getAppName() +
                ", AppVersion: " +
                event.getAppVersion() +
                ", ElementId: " +
                bpmnActivity.getElementId() +
                ", ActivityName: " +
                bpmnActivity.getActivityName() +
                ", ProcessDefinitionId: " +
                bpmnActivity.getProcessDefinitionId() +
                ", ProcessInstanceId: " +
                bpmnActivity.getProcessInstanceId() +
                ", ExecutionId: " +
                bpmnActivity.getExecutionId() +
                ", ProcessDefinitionKey: " +
                activityEvent.getProcessDefinitionKey() +
                ", ProcessDefinitionVersion: " +
                activityEvent.getProcessDefinitionVersion() +
                ", BusinessKey: " +
                activityEvent.getBusinessKey();

            logger.error("AAE-39416: Should create new ServiceTask: " + serviceTaskInfo);
        }

        return Optional.ofNullable(bpmnActivityEntity);
    }
}
