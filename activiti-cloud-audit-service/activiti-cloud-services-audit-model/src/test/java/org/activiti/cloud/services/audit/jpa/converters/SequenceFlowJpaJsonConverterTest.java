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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.activiti.test.Assertions.assertThat;

import org.activiti.api.process.model.BPMNElement;
import org.activiti.api.process.model.BPMNSequenceFlow;
import org.activiti.api.runtime.model.impl.BPMNSequenceFlowImpl;
import org.activiti.cloud.services.audit.jpa.converters.json.SequenceFlowJpaJsonConverter;
import org.junit.jupiter.api.Test;

public class SequenceFlowJpaJsonConverterTest {

    private SequenceFlowJpaJsonConverter converter = new SequenceFlowJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation() throws Exception {
        //given
        BPMNSequenceFlowImpl sequenceFlow = new BPMNSequenceFlowImpl(
            "sequence-flow-element-id",
            "source-element-id",
            "target-element-id"
        );

        sequenceFlow.setSourceActivityName("source-activity-name");
        sequenceFlow.setSourceActivityType("source-activity-type");
        sequenceFlow.setTargetActivityName("target-activity-name");
        sequenceFlow.setTargetActivityType("target-activity-type");
        sequenceFlow.setProcessDefinitionId("proc-def-id");
        sequenceFlow.setProcessInstanceId("proc-inst-id");
        //when
        String jsonRepresentation = converter.convertToDatabaseColumn(sequenceFlow);

        //then
        assertThatJson(jsonRepresentation).inPath("elementId").isEqualTo("sequence-flow-element-id");
        assertThatJson(jsonRepresentation).inPath("sourceActivityElementId").isEqualTo("source-element-id");
        assertThatJson(jsonRepresentation).inPath("sourceActivityName").isEqualTo("source-activity-name");
        assertThatJson(jsonRepresentation).inPath("sourceActivityType").isEqualTo("source-activity-type");
        assertThatJson(jsonRepresentation).inPath("targetActivityElementId").isEqualTo("target-element-id");
        assertThatJson(jsonRepresentation).inPath("targetActivityName").isEqualTo("target-activity-name");
        assertThatJson(jsonRepresentation).inPath("targetActivityType").isEqualTo("target-activity-type");
        assertThatJson(jsonRepresentation).inPath("processDefinitionId").isEqualTo("proc-def-id");
        assertThatJson(jsonRepresentation).inPath("processInstanceId").isEqualTo("proc-inst-id");
    }

    @Test
    public void convertToEntityAttributeShouldCreateAProcessInstanceWithFieldsSet() throws Exception {
        //given
        String jsonRepresentation =
            "{" +
            "\"elementId\":\"sequence-flow-element-id\"," +
            "\"sourceActivityElementId\":\"source-element-id\"," +
            "\"sourceActivityName\":\"source-activity-name\"," +
            "\"sourceActivityType\":\"source-activity-type\"," +
            "\"targetActivityElementId\":\"target-element-id\"," +
            "\"targetActivityName\":\"target-activity-name\"," +
            "\"targetActivityType\":\"target-activity-type\"," +
            "\"processDefinitionId\":\"proc-def-id\"," +
            "\"processInstanceId\":\"proc-inst-id\"}";

        //when
        BPMNSequenceFlow sequenceFlow = converter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(sequenceFlow)
            .isNotNull()
            .hasSourceActivityElementId("source-element-id")
            .hasSourceActivityName("source-activity-name")
            .hasSourceActivityType("source-activity-type")
            .hasTargetActivityElementId("target-element-id")
            .hasTargetActivityName("target-activity-name")
            .hasTargetActivityType("target-activity-type");

        assertThat((BPMNElement) sequenceFlow)
            .hasProcessDefinitionId("proc-def-id")
            .hasProcessInstanceId("proc-inst-id")
            .hasElementId("sequence-flow-element-id");
    }
}
