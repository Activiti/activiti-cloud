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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ConnectorTemplateResolverTest {

    private static final String PROJECT_RELEASE_DIR =
        "classpath:/orphaned-integration-recovery/project-release-volume/";
    private static final String VERSION_1_CONNECTORS = PROJECT_RELEASE_DIR + "1/connectors/";
    private static final String PROCESS_RESOURCE =
        "orphaned-integration-recovery/processes/ap-orchestrator-ox4ve.bpmn20.xml";
    private static final String FLOW_NODE_ID = "agenticTaskConnector_mraeqoyl";

    @Mock
    RepositoryService repositoryService;

    ConnectorTemplateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = resolverFor(new PathMatchingResourcePatternResolver(), PROJECT_RELEASE_DIR);
    }

    @Test
    void should_returnConnectorTemplate_when_flowNodeIsServiceTaskWithKnownConnector() {
        var processDefinitionId = givenProcessDefinition(1);

        assertThat(resolver.resolveTemplate(processDefinitionId, FLOW_NODE_ID)).contains("agenticTaskConnector");
    }

    @Test
    void should_readConnectorDefinitionsOfProcessDefinitionVersion() {
        var version1 = givenProcessDefinition(1);
        var version3 = givenProcessDefinition(3);

        assertThat(resolver.resolveTemplate(version1, FLOW_NODE_ID)).contains("agenticTaskConnector");
        assertThat(resolver.resolveTemplate(version3, FLOW_NODE_ID)).contains("agenticTaskConnectorV3");
    }

    @Test
    void should_readConnectorDefinitionsFromFileSystem_when_projectReleaseDirHasNoPrefix() throws IOException {
        var projectReleaseDir = new ClassPathResource("orphaned-integration-recovery/project-release-volume")
            .getFile()
            .getAbsolutePath();
        resolver = resolverFor(new PathMatchingResourcePatternResolver(), projectReleaseDir);
        var processDefinitionId = givenProcessDefinition(1);

        assertThat(resolver.resolveTemplate(processDefinitionId, FLOW_NODE_ID)).contains("agenticTaskConnector");
    }

    @Test
    void should_returnEmpty_when_flowNodeIsNotServiceTask() {
        var processDefinitionId = givenProcessDefinition(1);

        assertThat(resolver.resolveTemplate(processDefinitionId, "StartEvent_1")).isEmpty();
    }

    @Test
    void should_returnEmpty_when_flowNodeDoesNotExist() {
        var processDefinitionId = givenProcessDefinition(1);

        assertThat(resolver.resolveTemplate(processDefinitionId, "unknownFlowNode")).isEmpty();
    }

    @Test
    void should_returnEmpty_when_bpmnModelIsNotFound() {
        given(repositoryService.getBpmnModel(processDefinitionId(1))).willReturn(null);

        assertThat(resolver.resolveTemplate(processDefinitionId(1), FLOW_NODE_ID)).isEmpty();
    }

    @Test
    void should_returnEmpty_when_processDefinitionIdHasNoVersion() {
        assertThat(resolver.resolveTemplate("Process_1783414154972", FLOW_NODE_ID)).isEmpty();

        verify(repositoryService, never()).getBpmnModel(any());
    }

    @Test
    void should_returnEmpty_when_versionHasNoConnectorsDirectory() {
        var processDefinitionId = givenProcessDefinition(9);

        assertThat(resolver.resolveTemplate(processDefinitionId, FLOW_NODE_ID)).isEmpty();
    }

    @Test
    void should_ignoreConnectorDefinitions_when_theyAreIncomplete() {
        var processDefinitionId = givenProcessDefinition(2);

        assertThat(resolver.resolveTemplate(processDefinitionId, FLOW_NODE_ID)).isEmpty();
    }

    @Test
    void should_retryLoadingConnectorDefinitions_when_previousLoadingFailed() throws IOException {
        var resourceLoader = mock(ResourcePatternResolver.class);
        var delegate = new PathMatchingResourcePatternResolver();
        given(resourceLoader.getResource(VERSION_1_CONNECTORS)).willReturn(delegate.getResource(VERSION_1_CONNECTORS));
        given(resourceLoader.getResources(VERSION_1_CONNECTORS + "**.json"))
            .willThrow(new IOException("transient failure"))
            .willReturn(delegate.getResources(VERSION_1_CONNECTORS + "**.json"));
        resolver = resolverFor(resourceLoader, PROJECT_RELEASE_DIR);
        var processDefinitionId = givenProcessDefinition(1);

        assertThat(resolver.resolveTemplate(processDefinitionId, FLOW_NODE_ID)).isEmpty();
        assertThat(resolver.resolveTemplate(processDefinitionId, FLOW_NODE_ID)).contains("agenticTaskConnector");
    }

    @Test
    void should_loadConnectorDefinitionsOnlyOncePerVersion_when_resolvingMultipleTemplates() throws IOException {
        var resourceLoader = mock(ResourcePatternResolver.class);
        var delegate = new PathMatchingResourcePatternResolver();
        given(resourceLoader.getResource(VERSION_1_CONNECTORS)).willReturn(delegate.getResource(VERSION_1_CONNECTORS));
        given(resourceLoader.getResources(VERSION_1_CONNECTORS + "**.json")).willReturn(
            delegate.getResources(VERSION_1_CONNECTORS + "**.json")
        );
        resolver = resolverFor(resourceLoader, PROJECT_RELEASE_DIR);
        var processDefinitionId = givenProcessDefinition(1);

        resolver.resolveTemplate(processDefinitionId, FLOW_NODE_ID);
        var template = resolver.resolveTemplate(processDefinitionId, FLOW_NODE_ID);

        assertThat(template).contains("agenticTaskConnector");
        verify(resourceLoader).getResources(VERSION_1_CONNECTORS + "**.json");
    }

    @Test
    void should_extractVersion_when_processDefinitionIdIsValid() {
        assertThat(
            ConnectorTemplateResolver.toVersion("Process_1783414154972:5:3dffdec6-d371-11f1-be5f-b25359646a")
        ).contains(5);
        assertThat(ConnectorTemplateResolver.toVersion("Process_1783414154972:x:3dffdec6")).isEmpty();
        assertThat(ConnectorTemplateResolver.toVersion("Process_1783414154972:5")).isEmpty();
        assertThat(ConnectorTemplateResolver.toVersion(null)).isEmpty();
    }

    @Test
    void should_removeActionFromImplementation_when_extractingConnectorKey() {
        assertThat(ConnectorTemplateResolver.toConnectorKey("agentic-task-wvis1.AI_AGENT")).isEqualTo(
            "agentic-task-wvis1"
        );
        assertThat(ConnectorTemplateResolver.toConnectorKey("agentic-task-wvis1")).isEqualTo("agentic-task-wvis1");
    }

    private ConnectorTemplateResolver resolverFor(ResourcePatternResolver resourceLoader, String projectReleaseDir) {
        return new ConnectorTemplateResolver(
            repositoryService,
            JsonMapper.builder().build(),
            resourceLoader,
            projectReleaseDir
        );
    }

    private String processDefinitionId(int version) {
        return "Process_1783414154972:" + version + ":3dffdec6-d371-11f1-be5f-b25359646a";
    }

    private String givenProcessDefinition(int version) {
        var processDefinitionId = processDefinitionId(version);
        given(repositoryService.getBpmnModel(processDefinitionId)).willReturn(loadBpmnModel());
        return processDefinitionId;
    }

    private BpmnModel loadBpmnModel() {
        return new BpmnXMLConverter().convertToBpmnModel(
            () -> getClass().getClassLoader().getResourceAsStream(PROCESS_RESOURCE),
            false,
            false
        );
    }
}
