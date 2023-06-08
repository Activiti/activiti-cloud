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

import jakarta.persistence.EntityManager;
import java.util.Date;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCancelledEvent;
import org.activiti.cloud.services.query.model.BaseBPMNActivityEntity;

public class BPMNActivityCancelledEventHandler extends BaseBPMNActivityEventHandler implements QueryEventHandler {

    public BPMNActivityCancelledEventHandler(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityCancelledEvent activityEvent = CloudBPMNActivityCancelledEvent.class.cast(event);

        BaseBPMNActivityEntity bpmnActivityEntity = findOrCreateBPMNActivityEntity(event);

        bpmnActivityEntity.setCancelledDate(new Date(activityEvent.getTimestamp()));
        bpmnActivityEntity.setStatus(CloudBPMNActivity.BPMNActivityStatus.CANCELLED);

        entityManager.persist(bpmnActivityEntity);
    }

    @Override
    public String getHandledEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name();
    }
}
