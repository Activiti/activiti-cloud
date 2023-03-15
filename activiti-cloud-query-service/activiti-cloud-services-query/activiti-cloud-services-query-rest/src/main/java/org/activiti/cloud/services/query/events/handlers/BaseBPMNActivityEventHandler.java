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
package org.activiti.cloud.services.query.events.handlers;

import javax.persistence.EntityManager;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
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
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        String pkId = BPMNActivityEntity.IdBuilderHelper.from(bpmnActivity);

        BaseBPMNActivityEntity bpmnActivityEntity = null;

        if ("serviceTask".equals(bpmnActivity.getActivityType())) {
            bpmnActivityEntity = entityManager.find(ServiceTaskEntity.class, pkId);
        } else {
            bpmnActivityEntity = entityManager.find(BPMNActivityEntity.class, pkId);
        }

        if (bpmnActivityEntity == null) {
            bpmnActivityEntity = createBpmnActivityEntity(event);
        }

        return bpmnActivityEntity;
    }

    public BaseBPMNActivityEntity createBpmnActivityEntity(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        String pkId = BPMNActivityEntity.IdBuilderHelper.from(bpmnActivity);

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
