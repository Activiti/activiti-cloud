/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.starter.tests.services.audit;

import org.activiti.cloud.starters.test.MockProcessEngineEvent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.AUDIT_PRODUCER_IT;
import static org.assertj.core.api.Assertions.*;

@Profile(AUDIT_PRODUCER_IT)
@Component
@EnableBinding(AuditConsumer.class)
public class AuditConsumerStreamHandler {

    private boolean messageReceived;

    @StreamListener(AuditConsumer.AUDIT_CONSUMER)
    public void recieve(MockProcessEngineEvent[] events) {
        assertThat(events).isNotNull();
        assertThat(events.length).isEqualTo(9);
        assertThat(events[0].getEventType()).isEqualTo("ProcessCreatedEvent");
        assertThat(events[1].getEventType()).isEqualTo("ProcessStartedEvent");
        assertThat(events[2].getEventType()).isEqualTo("ActivityStartedEvent");
        assertThat(events[3].getEventType()).isEqualTo("ActivityCompletedEvent");
        assertThat(events[4].getEventType()).isEqualTo("SequenceFlowTakenEvent");
        assertThat(events[5].getEventType()).isEqualTo("ActivityStartedEvent");
        assertThat(events[6].getEventType()).isEqualTo("TaskCandidateGroupAddedEvent");
        assertThat(events[7].getEventType()).isEqualTo("TaskCandidateUserAddedEvent");
        assertThat(events[8].getEventType()).isEqualTo("TaskCreatedEvent");
        messageReceived = true;
    }

    public boolean isMessageReceived() {
        return messageReceived;
    }
}