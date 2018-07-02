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

package org.activiti.cloud.starters.test.builder;

import org.activiti.cloud.starters.test.MockIntegrationEvent;

public class MockIntegrationEventBuilder extends MockProcessEngineEventBuilder<MockIntegrationEvent, MockIntegrationEventBuilder> {

    public static MockIntegrationEventBuilder anIntegrationRequestSentEvent() {
        return new MockIntegrationEventBuilder(System.currentTimeMillis(),
                                          "IntegrationRequestSentEvent");
    }

    public static MockIntegrationEventBuilder anIntegrationResultRecievedEvent() {
        return new MockIntegrationEventBuilder(System.currentTimeMillis(),
                                          "IntegrationResultReceivedEvent");
    }

    private MockIntegrationEventBuilder(Long timestamp,
                                          String eventType) {
        super(timestamp,
              eventType);
    }

    public MockIntegrationEventBuilder withFlowNodeId(String flowNodeId) {
        getEvent().setFlowNodeId(flowNodeId);
        return this;
    }

    @Override
    protected MockIntegrationEvent createInstance(Long timestamp,
                                                  String eventType) {
        return new MockIntegrationEvent(timestamp, eventType);
    }
}
