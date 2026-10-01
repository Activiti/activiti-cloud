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
package org.activiti.cloud.api.task.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

public class CloudTaskImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldCopyAllowSelfServiceFromTask() {
        TaskImpl task = new TaskImpl("taskId", "my task", Task.TaskStatus.CREATED);
        task.setAllowSelfService(true);

        CloudTaskImpl cloudTask = new CloudTaskImpl(task);

        assertThat(cloudTask.isAllowSelfService()).isTrue();
    }

    @Test
    public void shouldDefaultAllowSelfServiceToFalse() {
        TaskImpl task = new TaskImpl("taskId", "my task", Task.TaskStatus.CREATED);

        CloudTaskImpl cloudTask = new CloudTaskImpl(task);

        assertThat(cloudTask.isAllowSelfService()).isFalse();
    }

    @Test
    public void shouldPreserveAllowSelfServiceThroughJsonRoundTrip() {
        TaskImpl task = new TaskImpl("taskId", "my task", Task.TaskStatus.CREATED);
        task.setAllowSelfService(true);
        CloudTaskImpl cloudTask = new CloudTaskImpl(task);

        String json = objectMapper.writeValueAsString(cloudTask);
        CloudTaskImpl deserialized = objectMapper.readValue(json, CloudTaskImpl.class);

        assertThat(deserialized.isAllowSelfService()).isTrue();
    }
}
