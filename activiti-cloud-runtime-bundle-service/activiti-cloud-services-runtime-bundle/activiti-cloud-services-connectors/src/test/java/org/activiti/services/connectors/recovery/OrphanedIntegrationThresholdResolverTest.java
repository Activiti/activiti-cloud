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
package org.activiti.services.connectors.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.RepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrphanedIntegrationThresholdResolverTest {

    private static final String PROCESS_RESOURCE =
        "orphaned-integration-recovery/processes/ap-orchestrator-ox4ve.bpmn20.xml";
    private static final String PROCESS_DEFINITION_ID = "Process_1783414154972:1:3dffdec6";

    @Mock
    RepositoryService repositoryService;

    @Test
    void should_returnThreshold_when_serviceTaskDeclaresIt() {
        givenBpmnModel();

        assertThat(resolve("agenticTaskConnector_mraeqoyl")).contains(5400);
    }

    @Test
    void should_returnEmpty_when_serviceTaskDoesNotDeclareThreshold() {
        givenBpmnModel();

        assertThat(resolve("restConnector_default")).isEmpty();
    }

    @Test
    void should_returnEmpty_when_thresholdIsNotANumber() {
        givenBpmnModel();

        assertThat(resolve("restConnector_invalid")).isEmpty();
    }

    @Test
    void should_returnEmpty_when_flowNodeIsNotServiceTask() {
        givenBpmnModel();

        assertThat(resolve("StartEvent_1")).isEmpty();
    }

    @Test
    void should_returnEmpty_when_flowNodeDoesNotExist() {
        givenBpmnModel();

        assertThat(resolve("unknownFlowNode")).isEmpty();
    }

    @Test
    void should_returnEmpty_when_bpmnModelIsNotFound() {
        given(repositoryService.getBpmnModel(PROCESS_DEFINITION_ID)).willReturn(null);

        assertThat(resolve("agenticTaskConnector_mraeqoyl")).isEmpty();
    }

    private Optional<Integer> resolve(String flowNodeId) {
        return new OrphanedIntegrationThresholdResolver(repositoryService).resolveThresholdSeconds(
            PROCESS_DEFINITION_ID,
            flowNodeId
        );
    }

    private void givenBpmnModel() {
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(
            () -> getClass().getClassLoader().getResourceAsStream(PROCESS_RESOURCE),
            false,
            false
        );
        given(repositoryService.getBpmnModel(PROCESS_DEFINITION_ID)).willReturn(bpmnModel);
    }
}
