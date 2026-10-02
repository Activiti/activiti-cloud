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
package org.activiti.cloud.services.query.events.handlers;

import jakarta.persistence.EntityManager;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BaseBPMNActivityEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class BaseBPMNActivityEventHandler {

    protected final EntityManager entityManager;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public BaseBPMNActivityEventHandler(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    protected BaseBPMNActivityEntity findOrCreateBPMNActivityEntity(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        String pkId = BPMNActivityEntity.IdBuilderHelper.from(bpmnActivity);

        Class<? extends BaseBPMNActivityEntity> entityClass = "serviceTask".equals(bpmnActivity.getActivityType())
            ? ServiceTaskEntity.class
            : BPMNActivityEntity.class;

        BaseBPMNActivityEntity bpmnActivityEntity = entityManager.find(entityClass, pkId);

        if (bpmnActivityEntity == null) {
            claimRow(event);
            bpmnActivityEntity = entityManager.find(entityClass, pkId);
        }

        return bpmnActivityEntity;
    }

    private void claimRow(CloudRuntimeEvent<?, ?> event) {
        try {
            requiresNewTransactionTemplate.executeWithoutResult(status -> {
                entityManager.persist(createBpmnActivityEntity(event));
                entityManager.flush();
            });
        } catch (RuntimeException ex) {
            if (!isConstraintViolation(ex)) {
                throw ex;
            }
        }
    }

    private static boolean isConstraintViolation(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException) {
                return true;
            }
        }
        return false;
    }

    public BaseBPMNActivityEntity createBpmnActivityEntity(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        String pkId = BPMNActivityEntity.IdBuilderHelper.from(bpmnActivity);

        BaseBPMNActivityEntity bpmnActivityEntity;

        if ("serviceTask".equals(bpmnActivity.getActivityType())) {
            bpmnActivityEntity = new ServiceTaskEntity(
                event.getServiceName(),
                event.getServiceFullName(),
                event.getServiceVersion(),
                event.getAppName(),
                event.getAppVersion()
            );
        } else {
            bpmnActivityEntity = new BPMNActivityEntity(
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
