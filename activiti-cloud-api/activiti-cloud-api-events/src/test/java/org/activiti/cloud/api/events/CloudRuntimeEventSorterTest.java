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
package org.activiti.cloud.api.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.junit.jupiter.api.Test;

class CloudRuntimeEventSorterTest {

    private static final String TASK_ID = "task-id";
    private static final String PROCESS_INSTANCE_ID = "process-instance-id";

    @Test
    void should_placeTaskCreatedBeforeItsVariables_when_theEngineEmittedTheVariablesFirst() {
        var firstVariable = variableCreated("var-1", 100L);
        var secondVariable = variableCreated("var-2", 200L);
        var taskCreated = taskCreated(300L);

        var sorted = CloudRuntimeEventSorter.sort(List.of(firstVariable, secondVariable, taskCreated));

        assertThat(sorted).containsExactly(taskCreated, firstVariable, secondVariable);
    }

    @Test
    void should_orderByTimestamp_when_eventsShareTheSamePriority() {
        var later = variableCreated("var-later", 500L);
        var earlier = variableCreated("var-earlier", 100L);

        var sorted = CloudRuntimeEventSorter.sort(List.of(later, earlier));

        assertThat(sorted).containsExactly(earlier, later);
    }

    @Test
    void should_applyTheDefaultPriority_when_theEventClassIsNotMapped() {
        var unmapped = processDeployed(900L);
        var taskCreated = taskCreated(100L);

        var sorted = CloudRuntimeEventSorter.sort(List.of(taskCreated, unmapped));

        // PROCESS_DEPLOYED is not in the priority map, so it falls back to the CloudRuntimeEvent priority of 0
        assertThat(sorted).containsExactly(unmapped, taskCreated);
    }

    @Test
    void should_returnANewList_when_sorting() {
        var events = List.<CloudRuntimeEvent<?, ?>>of(
            new CloudProcessCreatedEventImpl("id", 100L, new ProcessInstanceImpl())
        );

        var sorted = CloudRuntimeEventSorter.sort(events);

        assertThat(sorted).isNotSameAs(events).isEqualTo(events);
    }

    private CloudVariableCreatedEventImpl variableCreated(String name, long timestamp) {
        VariableInstance variable = new VariableInstanceImpl<>(name, String.class.getName(), "value", TASK_ID, TASK_ID);
        return new CloudVariableCreatedEventImpl("event-" + name, timestamp, variable);
    }

    private CloudTaskCreatedEventImpl taskCreated(long timestamp) {
        var task = new TaskImpl(TASK_ID, "task name", Task.TaskStatus.CREATED);
        task.setProcessInstanceId(PROCESS_INSTANCE_ID);
        return new CloudTaskCreatedEventImpl("event-task-created", timestamp, task);
    }

    private CloudProcessDeployedEventImpl processDeployed(long timestamp) {
        ProcessDefinition processDefinition = new ProcessDefinitionImpl();
        return new CloudProcessDeployedEventImpl("event-process-deployed", timestamp, processDefinition);
    }
}
