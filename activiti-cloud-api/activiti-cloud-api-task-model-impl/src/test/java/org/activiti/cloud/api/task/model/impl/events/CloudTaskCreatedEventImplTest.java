/*
 * Copyright 2017-2020 Alfresco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.api.task.model.impl.events;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudTaskCreatedEventImplTest {

    @Test
    public void shouldSetFlattenInfoBasedOnEntity() {
        //given
        TaskImpl task = new TaskImpl("taskId", "my task", Task.TaskStatus.CREATED);
        task.setProcessDefinitionId("procDefId");
        task.setProcessInstanceId("procInstId");
        task.setProcessDefinitionVersion(10);
        task.setBusinessKey("businessKey");

        //when
        CloudTaskCreatedEventImpl taskCreatedEvent = new CloudTaskCreatedEventImpl(task);

        //then
        assertThat(taskCreatedEvent.getEntityId()).isEqualTo("taskId");
        assertThat(taskCreatedEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(taskCreatedEvent.getProcessInstanceId()).isEqualTo("procInstId");
        assertThat(taskCreatedEvent.getProcessDefinitionVersion()).isEqualTo(10);
        assertThat(taskCreatedEvent.getBusinessKey()).isEqualTo("businessKey");
    }
}
