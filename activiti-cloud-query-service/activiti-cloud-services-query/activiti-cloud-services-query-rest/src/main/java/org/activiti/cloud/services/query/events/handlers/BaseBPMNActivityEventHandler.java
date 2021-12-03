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

import org.activiti.api.process.model.BPMNActivity;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;

import javax.persistence.EntityManager;

public abstract class BaseBPMNActivityEventHandler {

    protected final BPMNActivityRepository bpmnActivitiyRepository;
    protected final EntityManager entityManager;

    public BaseBPMNActivityEventHandler(BPMNActivityRepository activitiyRepository,
                                        EntityManager entityManager) {
        this.bpmnActivitiyRepository = activitiyRepository;
        this.entityManager = entityManager;
    }

    protected BPMNActivityEntity findOrCreateBPMNActivityEntity(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        String pkId = getBpmnActivityPk(bpmnActivity);

        BPMNActivityEntity bpmnActivityEntity = entityManager.find(BPMNActivityEntity.class,
                                                                   pkId);
        if (bpmnActivityEntity == null) {
            bpmnActivityEntity = createBpmnActivityEntity(event);
        }

        return bpmnActivityEntity;

    }

    public BPMNActivityEntity createBpmnActivityEntity(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityEvent activityEvent = CloudBPMNActivityEvent.class.cast(event);

        BPMNActivity bpmnActivity = activityEvent.getEntity();

        String pkId = getBpmnActivityPk(bpmnActivity);

        BPMNActivityEntity bpmnActivityEntity = new BPMNActivityEntity(event.getServiceName(),
                                                                       event.getServiceFullName(),
                                                                       event.getServiceVersion(),
                                                                       event.getAppName(),
                                                                       event.getAppVersion());
        // Let use event id to persist activity id
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

    protected String getBpmnActivityPk(BPMNActivity bpmnActivity) {
        return new StringBuilder().append(bpmnActivity.getProcessInstanceId())
                                  .append(":")
                                  .append(bpmnActivity.getElementId())
                                  .append(":")
                                  .append(bpmnActivity.getExecutionId())
                                  .toString();
    }

}
