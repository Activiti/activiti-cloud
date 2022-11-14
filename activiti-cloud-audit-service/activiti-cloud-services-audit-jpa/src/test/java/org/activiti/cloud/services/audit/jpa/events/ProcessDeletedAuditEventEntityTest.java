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
package org.activiti.cloud.services.audit.jpa.events;

import static org.activiti.test.Assertions.assertThat;

import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.junit.jupiter.api.Test;

public class ProcessDeletedAuditEventEntityTest {

    @Test
    public void shouldSetAllInformationAvailableFromCloudEvent() {
        //given
        long timestamp = System.currentTimeMillis();
        CloudProcessDeletedEventImpl processDeletedEvent = new CloudProcessDeletedEventImpl("eventId",
                                                                                            timestamp,
                                                                                            new ProcessInstanceImpl());
        processDeletedEvent.setAppName("appName");
        processDeletedEvent.setAppVersion("appV1");
        processDeletedEvent.setServiceName("serviceName");
        processDeletedEvent.setServiceFullName("serviceFN");
        processDeletedEvent.setServiceType("Audit");
        processDeletedEvent.setServiceVersion("serviceV2");
        processDeletedEvent.setMessageId("messageId");
        processDeletedEvent.setSequenceNumber(3);
        processDeletedEvent.setEntityId("entityID");
        processDeletedEvent.setProcessInstanceId("procInstId");
        processDeletedEvent.setProcessDefinitionId("procDefId");
        processDeletedEvent.setProcessDefinitionKey("procDefKey");
        processDeletedEvent.setBusinessKey("BusinessKey");
        processDeletedEvent.setParentProcessInstanceId("parentProcId");

        //when
        ProcessDeletedAuditEventEntity auditEventEntity = new ProcessDeletedAuditEventEntity(processDeletedEvent);

        //then
        assertThat(auditEventEntity)
                .hasEventId(processDeletedEvent.getId())
                .hasTimestamp(timestamp)
                .hasEventType(processDeletedEvent.getEventType().name())
                .hasAppName("appName")
                .hasAppVersion("appV1")
                .hasServiceName("serviceName")
                .hasServiceFullName("serviceFN")
                .hasServiceType("Audit")
                .hasServiceVersion("serviceV2")
                .hasMessageId("messageId")
                .hasSequenceNumber(3)
                .hasEntityId("entityID")
                .hasProcessInstanceId("procInstId")
                .hasProcessDefinitionId("procDefId")
                .hasProcessDefinitionKey("procDefKey")
                .hasBusinessKey("BusinessKey")
                .hasParentProcessInstanceId("parentProcId");
    }
}
