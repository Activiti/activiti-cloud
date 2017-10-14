/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.starters.test;

public class ActivityEventBuilder {

    private MockActivityEvent mockActivityEvent;

    private ActivityEventBuilder(Long timestamp, String eventType) {
        mockActivityEvent = new MockActivityEvent(timestamp, eventType);
    }

    public static ActivityEventBuilder aActivityStartedEvent(Long timestamp) {
        return new ActivityEventBuilder(timestamp, "ActivityStartedEvent");
    }

    public ActivityEventBuilder withExecutionId(String executionId) {
        mockActivityEvent.setExecutionId(executionId);
        return this;
    }

    public ActivityEventBuilder withProcessDefinitionId(String processDefinitionId) {
        mockActivityEvent.setProcessDefinitionId(processDefinitionId);
        return this;
    }

    public ActivityEventBuilder withProcessInstanceId(String processInstanceId) {
        mockActivityEvent.setProcessInstanceId(processInstanceId);
        return this;
    }

    public ActivityEventBuilder withName(String name) {
        mockActivityEvent.setActivityName(name);
        return this;
    }

    public MockActivityEvent build() {
        return mockActivityEvent;
    }
}
