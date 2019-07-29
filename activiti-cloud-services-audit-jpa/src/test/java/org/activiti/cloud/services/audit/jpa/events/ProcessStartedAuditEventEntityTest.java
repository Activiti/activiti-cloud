/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.audit.jpa.events;

import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.junit.Test;

import static org.activiti.test.Assertions.assertThat;

public class ProcessStartedAuditEventEntityTest {

    @Test
    public void shouldSetAllInformationAvailableFromCloudEvent() {
        //given
        long timestamp = System.currentTimeMillis();
        CloudProcessStartedEventImpl processStartedEvent = new CloudProcessStartedEventImpl("eventId",
                                                                                            timestamp,
                                                                                            new ProcessInstanceImpl());
        processStartedEvent.setAppName("appName");
        processStartedEvent.setAppVersion("appV1");
        processStartedEvent.setServiceName("serviceName");
        processStartedEvent.setServiceFullName("serviceFN");
        processStartedEvent.setServiceType("Audit");
        processStartedEvent.setServiceVersion("serviceV2");
        processStartedEvent.setMessageId("messageId");
        processStartedEvent.setSequenceNumber(3);
        processStartedEvent.setEntityId("entityID");
        processStartedEvent.setProcessInstanceId("procInstId");
        processStartedEvent.setProcessDefinitionId("procDefId");
        processStartedEvent.setProcessDefinitionKey("procDefKey");
        processStartedEvent.setBusinessKey("BusinessKey");
        processStartedEvent.setParentProcessInstanceId("parentProcId");

        //when
        ProcessStartedAuditEventEntity auditEventEntity = new ProcessStartedAuditEventEntity(processStartedEvent);

        //then
        assertThat(auditEventEntity)
                .hasEventId(processStartedEvent.getId())
                .hasTimestamp(timestamp)
                .hasEventType(processStartedEvent.getEventType().name())
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