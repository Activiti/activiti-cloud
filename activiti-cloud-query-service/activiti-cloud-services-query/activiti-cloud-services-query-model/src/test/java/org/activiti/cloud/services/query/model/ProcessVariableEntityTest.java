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

class ProcessVariableEntityTest {

    @Test
    void should_createProcessVariableUsingEventAttributes_when_variablesIsNotEphemeral() {
        //given
        CloudVariableCreatedEventImpl variableEvent = buildVariableCreatedEvent(false);

        //when
        ProcessVariableEntity processVariableEntity = new ProcessVariableEntity(variableEvent);

        //then
        assertHasSameAttributeValues(processVariableEntity, variableEvent);
    }

    @Test
    void should_createProcessVariableUsingEventAttributes_when_variableIsEphemeral() {
        //given
        CloudVariableCreatedEventImpl variableEvent = buildVariableCreatedEvent(true);

        //when
        ProcessVariableEntity processVariableEntity = new ProcessVariableEntity(variableEvent);

        //then
        assertHasSameAttributeValues(processVariableEntity, variableEvent);
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
        ProcessVariableEntity processVariableEntity,
        CloudVariableCreatedEventImpl variableEvent
    ) {
        assertThat(processVariableEntity.getServiceName()).isEqualTo(variableEvent.getServiceName());
        assertThat(processVariableEntity.getServiceFullName()).isEqualTo(variableEvent.getServiceFullName());
        assertThat(processVariableEntity.getServiceVersion()).isEqualTo(variableEvent.getServiceVersion());
        assertThat(processVariableEntity.getAppName()).isEqualTo(variableEvent.getAppName());
        assertThat(processVariableEntity.getAppVersion()).isEqualTo(variableEvent.getAppVersion());
        assertThat(processVariableEntity.getType()).isEqualTo(variableEvent.getEntity().getType());
        assertThat(processVariableEntity.getName()).isEqualTo(variableEvent.getEntity().getName());
        assertThat(processVariableEntity.getProcessInstanceId())
            .isEqualTo(variableEvent.getEntity().getProcessInstanceId());
        assertThat(processVariableEntity.getCreateTime()).isEqualTo(new Date(variableEvent.getTimestamp()));
        assertThat(processVariableEntity.getLastUpdatedTime()).isEqualTo(new Date(variableEvent.getTimestamp()));
        assertThat(processVariableEntity.<String>getValue()).isEqualTo(variableEvent.getEntity().getValue());
        assertThat(processVariableEntity.isEphemeral()).isEqualTo(variableEvent.isEphemeralVariable());
    }
}
