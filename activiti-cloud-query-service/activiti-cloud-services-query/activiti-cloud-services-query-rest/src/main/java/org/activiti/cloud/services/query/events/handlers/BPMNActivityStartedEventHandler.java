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

import java.util.Date;

import javax.persistence.EntityManager;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class BPMNActivityStartedEventHandler extends BaseBPMNActivityEventHandler implements QueryEventHandler {

    public BPMNActivityStartedEventHandler(BPMNActivityRepository activitiyRepository,
                                           EntityManager entityManager) {
        super(activitiyRepository,
              entityManager);
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudBPMNActivityStartedEvent activityEvent = CloudBPMNActivityStartedEvent.class.cast(event);

        BPMNActivityEntity bpmnActivityEntity = findOrCreateBPMNActivityEntity(event);

        // Activity can be cyclical, so we just update the status and started date anyways
        bpmnActivityEntity.setStartedDate(new Date(activityEvent.getTimestamp()));
        bpmnActivityEntity.setStatus(CloudBPMNActivity.BPMNActivityStatus.STARTED);
    }

    @Override
    public String getHandledEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name();
    }
}
