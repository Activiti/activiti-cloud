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

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.test.Assertions.assertThat;

import org.activiti.api.task.model.TaskCandidateGroup;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.cloud.services.audit.jpa.converters.json.TaskCandidateGroupJpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.converters.json.TaskCandidateUserJpaJsonConverter;
import org.junit.Test;

public class TaskCandidatesJpaJsonConverterTest {

    private TaskCandidateUserJpaJsonConverter converterCandidateUser = new TaskCandidateUserJpaJsonConverter();
    private TaskCandidateGroupJpaJsonConverter converterCandidateGroup = new TaskCandidateGroupJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation() throws Exception {
        //given
        TaskCandidateUserImpl candidateUser = new TaskCandidateUserImpl("user-id","task-id");
     
        //when
        String jsonRepresentation = converterCandidateUser.convertToDatabaseColumn(candidateUser);

        //then
        assertThatJson(jsonRepresentation)
                .node("userId").isEqualTo("user-id")
                .node("taskId").isEqualTo("task-id");
        
        //given
        TaskCandidateGroupImpl candidateGroup = new TaskCandidateGroupImpl("group-id","task-id");
     
        //when
        jsonRepresentation = converterCandidateGroup.convertToDatabaseColumn(candidateGroup);

        //then
        assertThatJson(jsonRepresentation)
                .node("groupId").isEqualTo("group-id")
                .node("taskId").isEqualTo("task-id");

    }

    @Test
    public void convertToEntityAttributeShouldCreateTaskCandidate() throws Exception {
        //given
        String jsonRepresentation =
                "{\"taskId\":\"task-id\"," +
                        "\"userId\":\"user-id\"}";

        //when
        TaskCandidateUserImpl candidateUser = (TaskCandidateUserImpl)converterCandidateUser.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(candidateUser)
                .isNotNull()
                .hasUserId("user-id");
        
        jsonRepresentation =
                "{\"taskId\":\"task-id\"," +
                        "\"groupId\":\"group-id\"}";
        
        jsonRepresentation =
                "{\"groupId\":\"group-id\"}";

        //when
        TaskCandidateGroup candidateGroup = converterCandidateGroup.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(candidateGroup)
                .isNotNull()
                .hasGroupId("group-id");
    }


}