/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.junit.jupiter.api.Test;

class TaskVariableEntityTest {

    @Test
    void should_createTaskVariableUsingEventAttributes_when_variablesIsNotEphemeral() {
        //given
        CloudVariableCreatedEventImpl variableEvent = buildVariableCreatedEvent(false);

        //when
        TaskVariableEntity taskVariableEntity = new TaskVariableEntity(variableEvent);

        //then
        assertHasSameAttributeValues(taskVariableEntity, variableEvent);
    }

    @Test
    void should_createProcessVariableUsingEventAttributes_when_variableIsEphemeral() {
        //given
        CloudVariableCreatedEventImpl variableEvent = buildVariableCreatedEvent(true);

        //when
        TaskVariableEntity taskVariableEntity = new TaskVariableEntity(variableEvent);

        //then
        assertHasSameAttributeValues(taskVariableEntity, variableEvent);
    }

    @Test
    void should_supportNullable_VariableIsEphemeralAttribute() {
        //given
        TaskVariableEntity taskVariableEntity = new TaskVariableEntity();

        //when
        taskVariableEntity.setEphemeral(null);

        //then
        assertThat(taskVariableEntity.isEphemeral()).isFalse();

        //when
        taskVariableEntity.setEphemeral(true);

        //then
        assertThat(taskVariableEntity.isEphemeral()).isTrue();

        //when
        taskVariableEntity.setEphemeral(false);

        //then
        assertThat(taskVariableEntity.isEphemeral()).isFalse();
    }

    private CloudVariableCreatedEventImpl buildVariableCreatedEvent(boolean ephemeral) {
        VariableInstanceImpl<String> variableInstance = new VariableInstanceImpl<>(
            "variable-name",
            "string",
            "any-string-value",
            "processInstanceId",
            "taskId"
        );
        CloudVariableCreatedEventImpl variableEvent = new CloudVariableCreatedEventImpl(variableInstance, ephemeral);
        variableEvent.setServiceName("service-name");
        variableEvent.setServiceFullName("service-full-name");
        variableEvent.setServiceVersion("1.0");
        variableEvent.setAppName("app-name");
        variableEvent.setAppVersion("2.0");
        return variableEvent;
    }

    private void assertHasSameAttributeValues(
        TaskVariableEntity taskVariableEntity,
        CloudVariableCreatedEventImpl variableEvent
    ) {
        assertThat(taskVariableEntity.getServiceName()).isEqualTo(variableEvent.getServiceName());
        assertThat(taskVariableEntity.getServiceFullName()).isEqualTo(variableEvent.getServiceFullName());
        assertThat(taskVariableEntity.getServiceVersion()).isEqualTo(variableEvent.getServiceVersion());
        assertThat(taskVariableEntity.getAppName()).isEqualTo(variableEvent.getAppName());
        assertThat(taskVariableEntity.getAppVersion()).isEqualTo(variableEvent.getAppVersion());
        assertThat(taskVariableEntity.getType()).isEqualTo(variableEvent.getEntity().getType());
        assertThat(taskVariableEntity.getName()).isEqualTo(variableEvent.getEntity().getName());
        assertThat(taskVariableEntity.getProcessInstanceId())
            .isEqualTo(variableEvent.getEntity().getProcessInstanceId());
        assertThat(taskVariableEntity.getCreateTime()).isEqualTo(new Date(variableEvent.getTimestamp()));
        assertThat(taskVariableEntity.getLastUpdatedTime()).isEqualTo(new Date(variableEvent.getTimestamp()));
        assertThat(taskVariableEntity.<String>getValue()).isEqualTo(variableEvent.getEntity().getValue());
        assertThat(taskVariableEntity.isEphemeral()).isEqualTo(variableEvent.isEphemeralVariable());
    }
}
