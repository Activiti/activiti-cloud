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

package org.activiti.cloud.services.audit.jpa.converters;

import java.util.Date;

import org.activiti.cloud.services.audit.jpa.converters.json.TaskJpaJsonConverter;
import org.activiti.api.task.model.Task;
import org.activiti.runtime.api.model.impl.TaskImpl;
import org.junit.Test;
import static org.activiti.test.Assertions.assertThat;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class TaskJpaJsonConverterTest {

    private TaskJpaJsonConverter converter = new TaskJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation() throws Exception {
        //given
        Date now = new Date();
        TaskImpl task = new TaskImpl("3", "task1",
                                     Task.TaskStatus.CREATED);
        task.setAssignee("user1");
        task.setDescription("First task");
        task.setOwner("user2");
        task.setPriority(50);
        task.setProcessDefinitionId("proc-def-id");
        task.setProcessInstanceId("10");
        task.setParentTaskId("parent-task-id");


        //when
        String jsonRepresentation = converter.convertToDatabaseColumn(task);

        //then
        assertThatJson(jsonRepresentation)
                .node("id").isEqualTo("\"3\"")
                .node("name").isEqualTo("task1")
                .node("processDefinitionId").isEqualTo("proc-def-id")
                .node("assignee").isEqualTo("user1")
                .node("description").isEqualTo("First task")
                .node("owner").isEqualTo("user2")
                .node("priority").isEqualTo("50")
                .node("processInstanceId").isEqualTo("\"10\"")
                .node("parentTaskId").isEqualTo("parent-task-id");
    }

    @Test
    public void convertToEntityAttributeShouldCreatedATaskInstanceWithFieldsSet() throws Exception {
        //given
        String jsonRepresentation =
                "{\"id\":\"3\"," +
                        "\"owner\":\"user2\"," +
                        "\"assignee\":\"user1\"," +
                        "\"name\":\"task1\"," +
                        "\"description\":\"First task\"," +
                        "\"priority\":50," +
                        "\"parentTaskId\":\"parent-task-id\"," +
                        "\"processDefinitionId\":\"proc-def-id\"," +
                        "\"processInstanceId\":\"10\"}";

        //when
        Task task = converter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(task)
                .isNotNull()
                .hasId("3")
                .hasOwner("user2")
                .hasAssignee("user1")
                .hasName("task1")
                .hasDescription("First task")
                .hasPriority(50)
                .hasParentTaskId("parent-task-id")
                .hasProcessDefinitionId("proc-def-id")
                .hasProcessInstanceId("10");
    }
}