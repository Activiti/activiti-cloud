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

import org.activiti.cloud.starters.test.MockProcessEngineEvent;

public abstract class MockProcessEngineEventBuilder<E extends MockProcessEngineEvent, B> {

    private E event;

    protected MockProcessEngineEventBuilder(Long timestamp, String eventType) {
        event = createInstance(timestamp, eventType);
    }

    protected abstract E createInstance(Long timestamp,
                                           String eventType);

    public B withExecutionId(String executionId) {
        event.setExecutionId(executionId);
        return (B) this;
    }

    public B withProcessDefinitionId(String processDefinitionId) {
        event.setProcessDefinitionId(processDefinitionId);
        return (B) this;
    }

    public B withProcessInstanceId(String processInstanceId) {
        event.setProcessInstanceId(processInstanceId);
        return (B) this;
    }

    public E getEvent() {
        return event;
    }

    public E build() {
        return getEvent();
    }
}
