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

import org.activiti.cloud.services.audit.jpa.converters.json.ProcessInstanceJpaJsonConverter;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.runtime.api.model.impl.ProcessInstanceImpl;
import org.junit.Test;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.test.Assertions.assertThat;

public class ProcessInstanceJpaJsonConverterTest {

    private ProcessInstanceJpaJsonConverter converter = new ProcessInstanceJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation() throws Exception {
        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("20");
        processInstance.setName("My instance");
        processInstance.setDescription("This is my process instance");
        processInstance.setProcessDefinitionId("proc-def-id");
        processInstance.setInitiator("initiator");
        processInstance.setStartDate(new Date());
        processInstance.setBusinessKey("business-key");
        processInstance.setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);



        //when
        String jsonRepresentation = converter.convertToDatabaseColumn(processInstance);

        //then
        assertThatJson(jsonRepresentation)
                .node("name").isEqualTo("My instance")
                .node("status").isEqualTo("RUNNING")
                .node("processDefinitionId").isEqualTo("proc-def-id")
                .node("businessKey").isEqualTo("business-key")
                .node("id").isEqualTo("\"20\"");
    }

    @Test
    public void convertToEntityAttributeShouldCreateAProcessInstanceWithFieldsSet() throws Exception {
        //given
        String jsonRepresentation =
                "{\"id\":\"20\"," +
                        "\"status\":\"RUNNING\"," +
                        "\"name\":\"My instance\"," +
                        "\"processDefinitionId\":\"proc-def-id\"}";

        //when
        ProcessInstance processInstance = converter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(processInstance)
                .isNotNull()
                .hasId("20")
                .hasStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
                .hasProcessDefinitionId("proc-def-id");
    }
}