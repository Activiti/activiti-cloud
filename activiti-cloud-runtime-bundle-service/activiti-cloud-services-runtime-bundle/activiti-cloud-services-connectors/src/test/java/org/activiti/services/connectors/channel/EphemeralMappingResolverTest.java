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
package org.activiti.services.connectors.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.ProcessVariablesMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EphemeralMappingResolverTest {

    private static final String PROC_DEF_ID = "procDefId";
    private static final String FLOW_NODE_ID = "serviceTask";

    @InjectMocks
    private EphemeralMappingResolver resolver;

    @Mock
    private ProcessExtensionService processExtensionService;

    @Test
    void should_returnTrue_when_mappingIsEphemeral() {
        givenMapping(FLOW_NODE_ID, true);

        assertThat(resolver.isEphemeral(buildEntity(PROC_DEF_ID, FLOW_NODE_ID))).isTrue();
    }

    @Test
    void should_returnFalse_when_mappingIsNotEphemeral() {
        givenMapping(FLOW_NODE_ID, false);

        assertThat(resolver.isEphemeral(buildEntity(PROC_DEF_ID, FLOW_NODE_ID))).isFalse();
    }

    @Test
    void should_returnFalse_when_noMappingForFlowNode() {
        givenMapping("anotherServiceTask", true);

        assertThat(resolver.isEphemeral(buildEntity(PROC_DEF_ID, FLOW_NODE_ID))).isFalse();
    }

    @Test
    void should_returnFalse_when_noExtensionsForProcessDefinition() {
        given(processExtensionService.getExtensionsForId(PROC_DEF_ID)).willReturn(new Extension());

        assertThat(resolver.isEphemeral(buildEntity(PROC_DEF_ID, FLOW_NODE_ID))).isFalse();
    }

    @Test
    void should_returnFalse_when_entityOrIdsAreMissing() {
        assertThat(resolver.isEphemeral(null)).isFalse();
        assertThat(resolver.isEphemeral(buildEntity(null, FLOW_NODE_ID))).isFalse();
        assertThat(resolver.isEphemeral(buildEntity(PROC_DEF_ID, null))).isFalse();
        verifyNoInteractions(processExtensionService);
    }

    @Test
    void should_returnTrue_when_mappingCannotBeResolved() {
        given(processExtensionService.getExtensionsForId(PROC_DEF_ID)).willThrow(
            new ActivitiObjectNotFoundException("not found")
        );

        assertThat(resolver.isEphemeral(buildEntity(PROC_DEF_ID, FLOW_NODE_ID))).isTrue();
    }

    private void givenMapping(String flowNodeId, boolean ephemeral) {
        ProcessVariablesMapping mapping = new ProcessVariablesMapping();
        mapping.setEphemeral(ephemeral);
        Extension extension = new Extension();
        extension.setMappings(Map.of(flowNodeId, mapping));
        given(processExtensionService.getExtensionsForId(PROC_DEF_ID)).willReturn(extension);
    }

    private IntegrationContextEntityImpl buildEntity(String processDefinitionId, String flowNodeId) {
        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        entity.setProcessDefinitionId(processDefinitionId);
        entity.setFlowNodeId(flowNodeId);
        return entity;
    }
}
