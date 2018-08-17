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

import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.BPMNElement;
import org.activiti.cloud.services.audit.jpa.converters.json.ActivityJpaJsonConverter;
import org.activiti.runtime.api.model.impl.BPMNActivityImpl;
import org.junit.Test;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.test.Assertions.assertThat;

public class BPMNActivityJpaJsonConverterTest {

    private ActivityJpaJsonConverter converter = new ActivityJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation() throws Exception {
        //given
        BPMNActivityImpl bpmnActivity = new BPMNActivityImpl("element-id",
                                                             "BPMN Activity Name",
                                                             "Service Task");
        bpmnActivity.setProcessDefinitionId("proc-def-id");
        bpmnActivity.setProcessInstanceId("proc-inst-id");

        //when
        String jsonRepresentation = converter.convertToDatabaseColumn(bpmnActivity);

        //then
        assertThatJson(jsonRepresentation)
                .node("elementId").isEqualTo("element-id")
                .node("activityName").isEqualTo("BPMN Activity Name")
                .node("activityType").isEqualTo("Service Task")
                .node("processDefinitionId").isEqualTo("proc-def-id")
                .node("processInstanceId").isEqualTo("proc-inst-id");
    }

    @Test
    public void convertToEntityAttributeShouldCreateAProcessInstanceWithFieldsSet() throws Exception {
        //given
        String jsonRepresentation =
                "{\"elementId\":\"element-id\"," +
                        "\"activityName\":\"BPMN Activity Name\"," +
                        "\"activityType\":\"Service Task\"," +
                        "\"processDefinitionId\":\"proc-def-id\"," +
                        "\"processInstanceId\":\"proc-inst-id\"}";

        //when
        BPMNActivity bpmnActivity = converter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(bpmnActivity)
                .isNotNull()
                .hasActivityName("BPMN Activity Name")
                .hasActivityType("Service Task")
                .hasElementId("element-id");
        assertThat((BPMNElement)bpmnActivity)
                .hasProcessInstanceId("proc-inst-id")
                .hasProcessDefinitionId("proc-def-id");
    }
}