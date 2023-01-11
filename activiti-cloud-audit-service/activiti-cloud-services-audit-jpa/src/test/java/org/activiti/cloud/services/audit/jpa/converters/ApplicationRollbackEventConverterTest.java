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
package org.activiti.cloud.services.audit.jpa.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.process.model.events.ApplicationEvent.ApplicationEvents;
import org.activiti.api.runtime.model.impl.DeploymentImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudApplicationDeployedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.ApplicationDeployedAuditEventEntity;
import org.junit.jupiter.api.Test;

class ApplicationRollbackEventConverterTest {

    private ApplicationRollbackEventConverter eventConverter = new ApplicationRollbackEventConverter(new EventContextInfoAppender());

    @Test
    public void should_convertToAPIRollbackEvent() {
        ApplicationDeployedAuditEventEntity auditEventEntity = (ApplicationDeployedAuditEventEntity) eventConverter.convertToEntity(
            createRollbackEvent());

        CloudApplicationDeployedEventImpl event = (CloudApplicationDeployedEventImpl) eventConverter.convertToAPI(auditEventEntity);

        assertThat(event.getEventType()).isEqualTo(ApplicationEvents.APPLICATION_ROLLBACK);
    }

    private CloudApplicationDeployedEventImpl createRollbackEvent() {
        DeploymentImpl entity =  new DeploymentImpl();
        entity.setId("entityId");
        CloudApplicationDeployedEventImpl event =new CloudApplicationDeployedEventImpl("eventId",
            System.currentTimeMillis(),
            new DeploymentImpl(),
            ApplicationEvents.APPLICATION_ROLLBACK);

        //Set explicitly to be sure
        event.setSequenceNumber(0);

        return event;
    }
}
