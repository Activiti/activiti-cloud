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
package org.activiti.cloud.services.modeling.rest.controller;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.modeling.mock.MockFactory.connectorFileContent;
import static org.activiti.cloud.services.modeling.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.modeling.mock.MockFactory.multipartExtensionsFile;
import static org.activiti.cloud.services.modeling.mock.MockFactory.multipartProcessFile;
import static org.activiti.cloud.services.modeling.mock.MockFactory.processModel;
import static org.activiti.cloud.services.modeling.mock.MockFactory.processModelWithExtensions;
import static org.activiti.cloud.services.modeling.mock.MockFactory.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.process.Extensions;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.entity.ProjectEntity;
import org.activiti.cloud.services.modeling.jpa.ModelJpaRepository;
import org.activiti.cloud.services.modeling.jpa.ProjectJpaRepository;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.activiti.cloud.services.modeling.service.api.ModelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for validating model content
 */
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@WithMockModelerUser
@Transactional
public class ModelValidationControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ModelService modelService;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProcessModelType processModelType;

    @Autowired
    private ConnectorModelType connectorModelType;

    @Autowired
    private ModelJpaRepository modelJpaRepository;

    @Autowired
    private ProjectJpaRepository projectJpaRepository;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    public void cleanUp() {
        modelJpaRepository.deleteAll();
        projectJpaRepository.deleteAll();
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessModelWithValidContent() throws Exception {
        byte[] validContent = resourceAsByteArray("process/x-19022.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        Model processModel = createModel(validContent);

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file))
            .andExpect(status().isNoContent());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelWithNullServiceTaskContent()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/null-implementation-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        Model processModel = createModel(validContent);

        ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(1)
            .extracting(ModelValidationError::getDescription, ModelValidationError::getValidatorSetName)
            .contains(
                tuple(
                    "One of the attributes 'implementation', 'class', 'delegateExpression', 'type', 'operation', or " +
                    "'expression' is mandatory on serviceTask.",
                    "activiti-executable-process"
                )
            );
    }

    @Test
    public void should_throwBadRequestException_when_validatingProcessModelWithInvalidContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "diagram.bpm",
            CONTENT_TYPE_XML,
            "BPMN diagram".getBytes()
        );

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project, "process-model"));

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Xml content for the model is not valid."));
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidContent() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        modelService.importModel(
            project,
            connectorModelType,
            connectorFileContent("movies", resourceAsByteArray("connector/movies.json"))
        );
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(
                project,
                "process-model",
                new Extensions(),
                resourceAsByteArray("process/RankMovie.bpmn20.xml")
            )
        );
        MockMultipartFile file = multipartExtensionsFile(
            processModel,
            resourceAsByteArray("process-extensions/RankMovie-extensions.json")
        );

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file))
            .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidContentAndNoDefaultValues()
        throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        modelService.importModel(
            project,
            connectorModelType,
            connectorFileContent("movies", resourceAsByteArray("connector/movies.json"))
        );
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(
                project,
                "process-model",
                new Extensions(),
                resourceAsByteArray("process/RankMovie.bpmn20.xml")
            )
        );
        MockMultipartFile file = multipartExtensionsFile(
            processModel,
            resourceAsByteArray("process-extensions/RankMovie-extensions-no-default-values.json")
        );

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file))
            .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidContentAndMessagePayloadMapping()
        throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(
                project,
                "process-model",
                new Extensions(),
                resourceAsByteArray("process/message-payload-mapping.bpmn20.xml")
            )
        );
        MockMultipartFile file = multipartExtensionsFile(
            processModel,
            resourceAsByteArray("process-extensions/valid-extensions-with-message-payload-mapping.json")
        );

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file))
            .andExpect(status().isNoContent());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelWithInvalidName()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/invalid-process-name.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/invalid-process-name.bpmn20.xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(1)
            .extracting(ModelValidationError::getDescription, ModelValidationError::getValidatorSetName)
            .contains(tuple("Invalid service implementation on service 'Task_1spvopd'", "BPMN service task validator"));
    }

    private Model createModel(byte[] processContent) {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        ModelEntity generatedProcess = processModel(project, "process-model");
        generatedProcess.setContent(processContent);
        return modelRepository.createModel(generatedProcess);
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelWithEmptyCallActivity()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/call-activity-with-no-call-element.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/call-activity-with-no-call-element.bpmn20.xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(2)
            .extracting(ModelValidationError::getProblem, ModelValidationError::getValidatorSetName)
            .contains(
                tuple(
                    "Call activity element must reference a process id present in the current project.",
                    "Invalid call activity reference validator."
                ),
                tuple(
                    "No call element found for call activity 'Task_1mbp1v0' in process " +
                    "'process-1722e83c-8f2f-4ddb-85bd-563ef87d279e'",
                    "Call activity must have a call element validator."
                )
            );
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelWithInvalidSequenceFlow()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/invalid-sequence-flow.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/invalid-sequence-flow.bpmn20.xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(ModelValidationError::getProblem, ModelValidationError::getReferenceId)
            .contains(
                tuple("Sequence flow has no source reference", "sid-75BFD70C-7949-441E-B85A-11C29A9BA0CD"),
                tuple("Sequence flow has no target reference", "sid-75BFD70C-7949-441E-B85A-11C29A9BA0CD")
            );
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelEventWithInvalidFlow()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/invalid-flows.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/invalid-flows.bpmn20.xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple("Start event has no outgoing flow", "BPMN Start event validator", "StartEvent_16jstbd"),
                tuple("Start event should not have incoming flow", "BPMN Start event validator", "StartEvent_16jstbd"),
                tuple("End event has no incoming flow", "BPMN End event validator", "EndEvent_0amu64a"),
                tuple("Flow node has no incoming flow", "BPMN Intermediate Flow node validator", "Task_0w8xho6")
            );
    }

    @Test
    public void should_returnSuccessful_when_validatingProcessModelWithEventSubProcess() throws Exception {
        byte[] validContent = resourceAsByteArray("process/valid-event-subprocess.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/valid-event-subprocess.bpmn20.xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().is2xxSuccessful());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelWithEmbeddedSubProcessWithoutIncomingAndOutgoingFlows()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/invalid-embedded-sub-process.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/invalid-embedded-sub-process.bpmn20.xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(ModelValidationError::getProblem, ModelValidationError::getReferenceId)
            .contains(
                tuple("Flow node has no incoming flow", "SubProcess_1j83p8h"),
                tuple("Flow node has no outgoing flow", "SubProcess_1j83p8h")
            );
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelEventWithoutEndEvent()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/process-without-end-event.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/process-without-end-event.bpmn20" + ".xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    "Flow node has no outgoing flow",
                    "Flow node [name: 'TestTaskName', id: 'TestTaskId'] has to have an outgoing flow",
                    "TestTaskId"
                )
            );
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelWithInvalidCallActivityVariable()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/call-activity-with-invalid-variable-reference.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/call-activity-with-invalid-variable-reference" + ".bpmn20.xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(1)
            .extracting(ModelValidationError::getProblem, ModelValidationError::getValidatorSetName)
            .contains(
                tuple(
                    "Call activity element must reference a process id present in the current project.",
                    "Invalid call activity reference validator."
                )
            );
    }

    @Test
    public void should_returnSuccessful_when_validatingProcessModelWithValidCallActivityElement() throws Exception {
        byte[] validContent = resourceAsByteArray("process/RankMovie.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/call-activity-with-valid-call-element.bpmn20.xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().is2xxSuccessful());
    }

    @Test
    public void should_returnSuccessful_when_validatingProcessModelWithValidCallActivityVariable() throws Exception {
        byte[] validContent = resourceAsByteArray("process/call-activity-with-valid-variable-reference.bpmn20.xml");
        Model processModel = createModel(validContent);
        MockMultipartFile file = multipartProcessFile(
            processModel,
            resourceAsByteArray("process/call-activity-with-valid-variable-reference.bpmn20" + ".xml")
        );

        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().is2xxSuccessful());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidMappingContent()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-mapping-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().isBadRequest());
        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
            .isEqualTo("Semantic model validation errors encountered: 2 schema violations found");

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(2)
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsOnly(
                tuple(
                    "extraneous key [inputds] is not permitted",
                    "#/extensions/Process_test/mappings/ServiceTask_06crg3b: extraneous key [inputds] is " +
                    "not permitted"
                ),
                tuple(
                    "extraneous key [outputss] is not permitted",
                    "#/extensions/Process_test/mappings/ServiceTask_06crg3b: extraneous key [outputss] is" +
                    " not permitted"
                )
            );
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidStringVariableContent()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-string-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsExactly(
                tuple(
                    "expected type: String, found: Integer",
                    "Mismatch value type - stringVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). " +
                    "Expected type is string"
                )
            );
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidIntegerVariableContent()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-integer-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "Process-Model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsExactly(
                tuple(
                    "expected type: Integer, found: String",
                    "Mismatch value type - integerVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). " +
                    "Expected type is integer"
                ),
                tuple(
                    "string [aloha] does not match pattern ^\\$\\{(.*)[\\}]$",
                    "Value format in integerVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68) is not a " +
                    "valid expression"
                )
            );
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidBigdecimalVariableContent()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-decimal-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "Process-Model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsExactly(
                tuple(
                    "expected type: Number, found: String",
                    "Mismatch value type - decimalVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). " +
                    "Expected type is decimal"
                ),
                tuple(
                    "string [hello] does not match pattern ^\\$\\{(.*)[\\}]$",
                    "Value format in decimalVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68) is not a " +
                    "valid expression"
                )
            );
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidBooleanVariableContent()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-boolean-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsExactly(
                tuple(
                    "expected type: Boolean, found: Integer",
                    "Mismatch value type - booleanVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). " +
                    "Expected type is boolean"
                ),
                tuple(
                    "expected type: String, found: Integer",
                    "Value format in booleanVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68) is not a " +
                    "valid expression"
                )
            );
    }

    @Test
    public void should_returnSuccessful_when_validatingProcessExtensionsWithNonObjectVariableContent()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-object-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().is2xxSuccessful());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidDateVariableContent()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-date-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsExactly(
                tuple(
                    "expected type: String, found: Integer",
                    "Mismatch value type - dateVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is " +
                    "date"
                ),
                tuple(
                    "expected type: String, found: Integer",
                    "Value format in dateVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68) is not a valid expression"
                ),
                tuple(
                    "string [aloha] does not match pattern ^[0-9]{4}-(((0[13578]|(10|12))-(0[1-9]|[1-2][0-9]|3[0-1]))" +
                    "|(02-(0[1-9]|[1-2][0-9]))|((0[469]|11)-(0[1-9]|[1-2][0-9]|30)))$",
                    "Invalid date - dateVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68)"
                ),
                tuple(
                    "string [aloha] does not match pattern ^\\$\\{(.*)[\\}]$",
                    "Value format in dateVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68) is not a valid expression"
                )
            );
    }

    @Test
    public void should_throwNotFoundException_when_validatingModelThatNotExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "diagram.bpm", "text/plain", "BPMN diagram".getBytes());
        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate", "model_id").file(file))
            .andExpect(status().isNotFound());
    }

    @Test
    public void should_throwNotFoundException_when_validatingInvalidProcessModelUsingTextContentType()
        throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "diagram.bpmn20.xml",
            "text/plain",
            "BPMN diagram".getBytes()
        );

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project, "process-model"));

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Xml content for the model is not valid."));

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Xml content for the model is not valid."));
    }

    @Test
    public void should_returnStatusNoContent_when_validatingConnectorValidContent() throws Exception {
        byte[] validContent = resourceAsByteArray("connector/connector-simple.json");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "connector-simple.json",
            CONTENT_TYPE_JSON,
            validContent
        );
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model connectorModel = modelRepository.createModel(connectorModel(project, "connector-model"));

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate", connectorModel.getId()).file(file))
            .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingConnectorValidContentWithTemplate() throws Exception {
        byte[] validContent = resourceAsByteArray("connector/connector-template.json");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "connector-template.json",
            CONTENT_TYPE_JSON,
            validContent
        );

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model connectorModel = modelRepository.createModel(connectorModel(project, "connector-model"));

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate", connectorModel.getId()).file(file))
            .andExpect(status().isNoContent());
    }

    @Test
    public void should_throwException_when_validatingProcessWithServiceTaskImplementationSetToUnknownConnectorAction()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/unknown-implementation-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        Model processModel = createModel(validContent);

        ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(1)
            .extracting(ModelValidationError::getDescription, ModelValidationError::getValidatorSetName)
            .contains(
                tuple("Invalid service implementation on service 'ServiceTask_1qr4ad0'", "BPMN service task validator")
            );
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessWithServiceTaskImplementationSetToDMNAction()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/dmn-implementation-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        Model processModel = createModel(validContent);

        ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessWithServiceTaskImplementationSetToScriptActionWithCatchBoundary()
        throws Exception {
        byte[] validContent = resourceAsByteArray(
            "process/script-implementation-service-task-with-catch-boundary.bpmn20.xml"
        );
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        Model processModel = createModel(validContent);

        ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isNoContent());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessWithServiceTaskImplementationSetToScriptActionWithNoCatchBoundary()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/script-implementation-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        Model processModel = createModel(validContent);

        ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );
        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
            .contains("Semantic model validation warnings encountered: 1 warnings found");

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(1)
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::isWarning
            )
            .containsOnly(
                tuple(
                    "Missing Catch Error boundary event",
                    "The service implementation on service 'ServiceTask_1qr4ad0' might fail silently. " +
                    "Consider adding an Error boundary event to handle failures.",
                    true
                )
            );
    }

    @Test
    public void should_validateModelContentInTheProjectContext_when_projectIdIsProvided() throws Exception {
        ProjectEntity projectOne = (ProjectEntity) projectRepository.createProject(project("project-one"));
        ProjectEntity projectTwo = (ProjectEntity) projectRepository.createProject(project("project-two"));

        modelService.importSingleModel(
            projectOne,
            connectorModelType,
            connectorFileContent("movies", resourceAsByteArray("connector/movies.json"))
        );

        byte[] validContent = resourceAsByteArray("process/RankMovie.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        ModelEntity generatedProcess = processModel(projectOne, "process-model");
        generatedProcess.setContent(validContent);
        Model processModel = modelRepository.createModel(generatedProcess);

        processModel.setScope(ModelScope.GLOBAL);
        processModel.addProject(projectTwo);
        processModel = modelService.updateModel(processModel, processModel);

        mockMvc
            .perform(
                multipart(
                    "/v1/models/{model_id}/validate?projectId={project_id}",
                    processModel.getId(),
                    projectOne.getId()
                )
                    .file(file)
            )
            .andExpect(status().isNoContent());

        ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate?projectId={project_id}", processModel.getId(), projectTwo.getId())
                .file(file)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(1)
            .extracting(ModelValidationError::getDescription, ModelValidationError::getValidatorSetName)
            .contains(tuple("Invalid service implementation on service 'Task_1spvopd'", "BPMN service task validator"));
    }

    @Test
    public void should_validateModeExtensionslInTheProjectContext_when_projectIdIsProvided() throws Exception {
        ProjectEntity projectOne = (ProjectEntity) projectRepository.createProject(project("project-one"));
        ProjectEntity projectTwo = (ProjectEntity) projectRepository.createProject(project("project-two"));

        modelService.importSingleModel(
            projectOne,
            connectorModelType,
            connectorFileContent("movies", resourceAsByteArray("connector/movies.json"))
        );

        byte[] validContent = resourceAsByteArray("process/RankMovie.bpmn20.xml");
        FileContent file = new FileContent("process-model.bpmn20.xml", CONTENT_TYPE_XML, validContent);

        Model processModel = modelService.importSingleModel(projectOne, processModelType, file);

        processModel.setScope(ModelScope.GLOBAL);
        processModel.addProject(projectTwo);
        processModel = modelService.updateModel(processModel, processModel);

        MockMultipartFile extensionsFile = multipartExtensionsFile(
            processModel,
            resourceAsByteArray("process-extensions/RankMovie-extensions.json")
        );

        mockMvc
            .perform(
                multipart(
                    "/v1/models/{model_id}/validate/extensions?projectId={project_id}",
                    processModel.getId(),
                    projectOne.getId()
                )
                    .file(extensionsFile)
            )
            .andExpect(status().isNoContent());

        ResultActions resultActions = mockMvc.perform(
            multipart(
                "/v1/models/{model_id}/validate/extensions?projectId={project_id}",
                processModel.getId(),
                projectTwo.getId()
            )
                .file(extensionsFile)
        );

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(2)
            .extracting(ModelValidationError::getDescription)
            .contains(
                "The extensions for process 'Process_RankMovieId' contains INPUTS mappings to task 'Task_1spvopd' " +
                "referencing an unknown connector action 'movies.getMovieDesc'",
                "The extensions for process 'Process_RankMovieId' contains OUTPUTS mappings to task 'Task_1spvopd' " +
                "referencing an unknown connector action 'movies.getMovieDesc'"
            );
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidTemplateType() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(
                project,
                "Process_x",
                new Extensions(),
                resourceAsByteArray("process/x-19022.bpmn20.xml")
            )
        );

        MockMultipartFile file = multipartExtensionsFile(
            processModel,
            resourceAsByteArray("process-extensions/valid-templates-extensions.json")
        );

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file))
            .andExpect(status().isNoContent());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidTemplatesContent()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-templates-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().isBadRequest());
        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
            .isEqualTo("Semantic model validation errors encountered: 2 schema violations found");

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(2)
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsOnly(
                tuple(
                    "something is not a valid enum value",
                    "#/extensions/Process_test/templates/tasks/Task2/assignee/type: something is not a " +
                    "valid enum value"
                ),
                tuple(
                    "expected type: String, found: Null",
                    "#/extensions/Process_test/templates/tasks/Task1/assignee/value: expected type: " +
                    "String, found: Null"
                )
            );
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidExpressionAsIntegerVariableType()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray(
            "process-extensions/valid-extensions-with-variable-integer-as-expression.json"
        );
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file))
            .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidExpressionAsDecimalVariableType()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray(
            "process-extensions/valid-extensions-with-variable-decimal-as-expression.json"
        );
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );

        mockMvc
            .perform(multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file))
            .andExpect(status().isNoContent());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidExpressionAsIntegerVariableType()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray(
            "process-extensions/invalid-extensions-with-incomplete-integer-expression.json"
        );
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().isBadRequest());
        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
            .isEqualTo("Semantic model validation errors encountered: 2 schema violations found");

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(2)
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsOnly(
                tuple(
                    "string [${error] does not match pattern ^\\$\\{(.*)[\\}]$",
                    "Value format in var1(8b9ac008-8a76-4ebd-8221-04452add5f22) is not a valid expression"
                ),
                tuple(
                    "expected type: Integer, found: String",
                    "Mismatch value type - var1(8b9ac008-8a76-4ebd-8221-04452add5f22). Expected type is " + "integer"
                )
            );
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidExpressionAsDecimalVariableType()
        throws Exception {
        byte[] invalidContent = resourceAsByteArray(
            "process-extensions/invalid-extensions-with-incomplete-decimal-expression.json"
        );
        MockMultipartFile file = new MockMultipartFile("file", "extensions.json", CONTENT_TYPE_JSON, invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
            processModelWithExtensions(project, "process-model", new Extensions())
        );
        final ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate/extensions", processModel.getId()).file(file)
        );
        resultActions.andExpect(status().isBadRequest());
        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
            .isEqualTo("Semantic model validation errors encountered: 2 schema violations found");

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
            .hasSize(2)
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsOnly(
                tuple(
                    "string [${error] does not match pattern ^\\$\\{(.*)[\\}]$",
                    "Value format in var1(8b9ac008-8a76-4ebd-8221-04452add5f22) is not a valid expression"
                ),
                tuple(
                    "expected type: Number, found: String",
                    "Mismatch value type - var1(8b9ac008-8a76-4ebd-8221-04452add5f22). Expected type is " + "decimal"
                )
            );
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessWithServiceTaskImplementationSetToEmailService()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/email-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        Model processModel = createModel(validContent);

        ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessWithServiceTaskImplementationSetToDocgenService()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/docgen-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        Model processModel = createModel(validContent);

        ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessWithServiceTaskImplementationSetToHxPContentService()
        throws Exception {
        byte[] validContent = resourceAsByteArray("process/hxp-content-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file", "process.xml", CONTENT_TYPE_XML, validContent);
        Model processModel = createModel(validContent);

        ResultActions resultActions = mockMvc.perform(
            multipart("/v1/models/{model_id}/validate", processModel.getId()).file(file)
        );

        resultActions.andExpect(status().isNoContent());
    }
}
