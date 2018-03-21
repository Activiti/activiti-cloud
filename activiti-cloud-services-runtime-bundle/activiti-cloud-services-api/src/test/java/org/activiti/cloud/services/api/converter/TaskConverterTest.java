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

package org.activiti.cloud.services.api.converter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.api.model.converter.ListConverter;
import org.activiti.cloud.services.api.model.converter.TaskConverter;
import org.junit.Before;
import org.junit.Test;

import static org.activiti.cloud.services.api.converter.MockTaskBuilder.taskBuilder;
import static org.activiti.cloud.services.api.converter.MockTaskBuilder.taskEntityBuilder;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.CANCELLED;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.CREATED;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.SUSPENDED;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link TaskConverter}
 */
public class TaskConverterTest {

    private TaskConverter taskConverter;

    @Before
    public void setUp() {
        taskConverter = new TaskConverter(new ListConverter());
    }

    /**
     * Test that all task fields are present in the converted task
     */
    @Test
    public void testConvertFromTask() {
        //WHEN
        Date now = new Date();
        Task convertedTask = taskConverter.from(
                taskBuilder()
                        .withId("testTaskId")
                        .withAssignee("testUser")
                        .withName("testTaskName")
                        .withDescription("testTaskDescription")
                        .withCreatedDate(now)
                        .withClaimedDate(now)
                        .withDueDate(now)
                        .withPriority(112)
                        .withProcessDefinitionId("testProcessDefinitionId")
                        .withProcessInstanceId("testProcessInstanceId")
                        .withParentTaskId("testParentTaskId")
                        .build()
        );

        //THEN
        assertThat(convertedTask)
                .isNotNull()
                .extracting(Task::getId,
                            Task::getAssignee,
                            Task::getName,
                            Task::getDescription,
                            Task::getCreatedDate,
                            Task::getClaimedDate,
                            Task::getDueDate,
                            Task::getPriority,
                            Task::getProcessDefinitionId,
                            Task::getProcessInstanceId,
                            Task::getParentTaskId,
                            Task::getStatus)
                .containsExactly("testTaskId",
                                 "testUser",
                                 "testTaskName",
                                 "testTaskDescription",
                                 now,
                                 now,
                                 now,
                                 112,
                                 "testProcessDefinitionId",
                                 "testProcessInstanceId",
                                 "testParentTaskId",
                                 ASSIGNED);
    }

    /**
     * Test that all tasks from a list are converted when dedicated from() method id used
     */
    @Test
    public void testConvertFromTasksList() {
        //WHEN
        List<Task> convertedTasks = taskConverter.from(Arrays.asList(
                taskEntityBuilder().withId("testTaskId1").build(),
                taskEntityBuilder().withId("testTaskId2").build()
        ));

        //THEN
        assertThat(convertedTasks)
                .isNotEmpty()
                .extracting(Task::getId)
                .containsExactly("testTaskId1",
                                 "testTaskId2");
    }

    /**
     * Test that computed status for a cancelled task is CANCELLED
     */
    @Test
    public void testCalculateStatusCancelledTask() {
        assertThat(taskConverter.from(taskEntityBuilder().withCancalled(true).build()))
                .isNotNull()
                .extracting(Task::getStatus)
                .containsExactly(CANCELLED);
    }

    /**
     * Test that computed status for a suspended task is SUSPENDED
     */
    @Test
    public void testCalculateStatusSuspendedTask() {
        assertThat(taskConverter.from(taskEntityBuilder().withSuspended(true).build()))
                .isNotNull()
                .extracting(Task::getStatus)
                .containsExactly(SUSPENDED);
    }

    /**
     * Test that computed status for a assigned task is ASSIGNED
     */
    @Test
    public void testCalculateStatusAssignedTask() {
        assertThat(taskConverter.from(taskBuilder().withAssignee("testUser").build()))
                .isNotNull()
                .extracting(Task::getStatus)
                .containsExactly(ASSIGNED);
    }

    /**
     * Test that computed status for a not assigned task is CREATED
     */
    @Test
    public void testCalculateStatusCreatedTask() {
        assertThat(taskConverter.from(taskBuilder().build()))
                .isNotNull()
                .extracting(Task::getStatus)
                .containsExactly(CREATED);
    }
}
