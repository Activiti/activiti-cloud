/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.runtime.model.impl.ProcessCandidateStarterGroupImpl;
import org.activiti.api.runtime.model.impl.ProcessCandidateStarterUserImpl;
import org.activiti.cloud.services.audit.jpa.converters.json.ProcessCandidateStarterGroupJpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.converters.json.ProcessCandidateStarterUserJpaJsonConverter;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.test.Assertions.assertThat;

public class ProcessCandidateStartersJpaJsonConverterTest {

    private ProcessCandidateStarterUserJpaJsonConverter candidateStarterUserConverter = new ProcessCandidateStarterUserJpaJsonConverter();
    private ProcessCandidateStarterGroupJpaJsonConverter candidateStarterGroupConverter = new ProcessCandidateStarterGroupJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentationForUserConverter() throws Exception {
        //given
        ProcessCandidateStarterUserImpl candidateStarterUser = new ProcessCandidateStarterUserImpl("aprocessId", "auserId");

        //when
        String jsonRepresentation = candidateStarterUserConverter.convertToDatabaseColumn(candidateStarterUser);

        //then
        assertThatJson(jsonRepresentation)
                .node("userId").isEqualTo("auserId")
                .node("processDefinitionId").isEqualTo("aprocessId");

    }

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentationForGroupConverter() throws Exception {
        //given
        ProcessCandidateStarterGroupImpl candidateStarterGroup = new ProcessCandidateStarterGroupImpl("aprocessId", "agroupId");

        //when
        String jsonRepresentation = candidateStarterGroupConverter.convertToDatabaseColumn(candidateStarterGroup);

        //then
        assertThatJson(jsonRepresentation)
            .node("groupId").isEqualTo("agroupId")
            .node("processDefinitionId").isEqualTo("aprocessId");
    }

    @Test
    public void convertToEntityAttributeShouldCreateTaskCandidateForUserConvertor() throws Exception {
        //given
        String jsonRepresentation =
                "{\"processDefinitionId\":\"aprocessId\"," +
                        "\"userId\":\"auserId\"}";

        //when
        ProcessCandidateStarterUserImpl candidateStarterUser = (ProcessCandidateStarterUserImpl)candidateStarterUserConverter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(candidateStarterUser)
                .isNotNull()
                .hasUserId("auserId");

    }

    @Test
    public void convertToEntityAttributeShouldCreateTaskCandidateForGroupConvertor() throws Exception {
        //given
        String jsonRepresentation =
            "{\"processDefinitionId\":\"aprocessId\"," +
                "\"groupId\":\"agroupId\"}";

        //when
        ProcessCandidateStarterGroupImpl candidateStarterGroup = (ProcessCandidateStarterGroupImpl)candidateStarterGroupConverter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(candidateStarterGroup)
            .isNotNull()
            .hasGroupId("agroupId");

    }


}
