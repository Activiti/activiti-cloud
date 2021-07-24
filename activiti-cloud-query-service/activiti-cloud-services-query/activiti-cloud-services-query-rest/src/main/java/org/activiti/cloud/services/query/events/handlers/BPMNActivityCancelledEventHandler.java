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

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCancelledEvent;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.Optional;

@Transactional
public class BPMNActivityCancelledEventHandler extends BaseBPMNActivityEventHandler implements QueryEventHandler {

    public BPMNActivityCancelledEventHandler(BPMNActivityRepository activitiyRepository,
                                             EntityManager entityManager) {
        super(activitiyRepository,
              entityManager);

    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityCancelledEvent activityEvent = CloudBPMNActivityCancelledEvent.class.cast(event);

        BPMNActivityEntity bpmnActivityEntity = findOrCreateBPMNActivityEntity(event);

        boolean isFinalState = Optional.ofNullable(bpmnActivityEntity.getStatus())
                                       .map(CloudBPMNActivity.BPMNActivityStatus::isFinalState)
                                       .orElse(false);
        if (!isFinalState) {
            bpmnActivityEntity.setCancelledDate(new Date(activityEvent.getTimestamp()));
            bpmnActivityEntity.setStatus(CloudBPMNActivity.BPMNActivityStatus.CANCELLED);
        }
    }

    @Override
    public String getHandledEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name();
    }
}
