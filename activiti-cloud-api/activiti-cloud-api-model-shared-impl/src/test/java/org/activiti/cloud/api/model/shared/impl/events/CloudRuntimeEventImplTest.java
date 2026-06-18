/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.api.model.shared.impl.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.junit.jupiter.api.Test;

class CloudRuntimeEventImplTest {

    @Test
    void should_returnCommandId_when_commandIdIsSet() {
        var event = new CloudVariableCreatedEventImpl();

        event.setCommandId("cmd-123");

        assertThat(event.getCommandId()).isEqualTo("cmd-123");
    }

    @Test
    void should_notBeEqual_when_commandIdDiffers() {
        var variableInstance = new VariableInstanceImpl<Object>();
        var event1 = new CloudVariableCreatedEventImpl("event-id", 0L, variableInstance);
        event1.setCommandId("cmd-1");

        var event2 = new CloudVariableCreatedEventImpl("event-id", 0L, variableInstance);
        event2.setCommandId("cmd-2");

        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    void should_beEqual_when_commandIdMatches() {
        var variableInstance = new VariableInstanceImpl<Object>();
        var event1 = new CloudVariableCreatedEventImpl("event-id", 0L, variableInstance);
        event1.setCommandId("cmd-1");

        var event2 = new CloudVariableCreatedEventImpl("event-id", 0L, variableInstance);
        event2.setCommandId("cmd-1");

        assertThat(event1).isEqualTo(event2);
    }

    @Test
    void should_haveDifferentHashCode_when_commandIdDiffers() {
        var event1 = new CloudVariableCreatedEventImpl();
        event1.setCommandId("cmd-1");

        var event2 = new CloudVariableCreatedEventImpl();
        event2.setCommandId("cmd-2");

        assertThat(event1.hashCode()).isNotEqualTo(event2.hashCode());
    }

    @Test
    void should_includeCommandId_when_toStringIsCalled() {
        var event = new CloudVariableCreatedEventImpl();
        event.setCommandId("cmd-123");

        assertThat(event.toString()).contains("commandId=cmd-123");
    }
}
