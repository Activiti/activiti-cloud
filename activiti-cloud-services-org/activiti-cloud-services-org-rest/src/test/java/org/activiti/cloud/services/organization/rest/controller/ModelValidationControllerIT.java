/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.organization.rest.controller;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.mock.MockFactory.connectorFileContent;
import static org.activiti.cloud.services.organization.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.multipartExtensionsFile;
import static org.activiti.cloud.services.organization.mock.MockFactory.multipartProcessFile;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithExtensions;
import static org.activiti.cloud.services.organization.mock.MockFactory.project;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.api.process.Extensions;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ProjectEntity;
import org.activiti.cloud.services.organization.security.WithMockModelerUser;
import org.activiti.cloud.services.organization.service.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for validating model content
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
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

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessModelWithValidContent() throws Exception {
        byte[] validContent = resourceAsByteArray("process/x-19022.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "process.xml",
                                                       CONTENT_TYPE_XML,
                                                       validContent);
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  processModel.getId())
                                .file(file))
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelWithNullServiceTaskContent() throws Exception {
        byte[] validContent = resourceAsByteArray("process/null-implementation-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "process.xml",
                                                       CONTENT_TYPE_XML,
                                                       validContent);
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   API_VERSION,
                                   processModel.getId())
                                 .file(file));

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .hasSize(1)
                .extracting(ModelValidationError::getDescription,
                            ModelValidationError::getValidatorSetName)
                .contains(tuple("One of the attributes 'implementation', 'class', 'delegateExpression', 'type', 'operation', or 'expression' is mandatory on serviceTask.",
                                "activiti-executable-process"));
    }

    @Test
    public void should_throwBadRequestException_when_validatingProcessModelWithInvalidContent() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       CONTENT_TYPE_XML,
                                                       "BPMN diagram".getBytes());

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   API_VERSION,
                                   processModel.getId())
                                 .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidContent() throws Exception {

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        modelService.importModel(project,
                                 connectorModelType,
                                 connectorFileContent("movies",
                                                      resourceAsByteArray("connector/movies.json")));
        Model processModel = modelRepository.createModel(
                processModelWithExtensions(project,
                                           "process-model",
                                           new Extensions(),
                                           resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        MockMultipartFile file = multipartExtensionsFile(
                processModel,
                resourceAsByteArray("process-extensions/RankMovie-extensions.json"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate/extensions",
                                  API_VERSION,
                                  processModel.getId())
                                .file(file))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidContentAndNoDefaultValues() throws Exception {

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        modelService.importModel(project,
                                 connectorModelType,
                                 connectorFileContent("movies",
                                                      resourceAsByteArray("connector/movies.json")));
        Model processModel = modelRepository.createModel(
                processModelWithExtensions(project,
                                           "process-model",
                                           new Extensions(),
                                           resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        MockMultipartFile file = multipartExtensionsFile(
                processModel,
                resourceAsByteArray("process-extensions/RankMovie-extensions-no-default-values.json"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate/extensions",
                                  API_VERSION,
                                  processModel.getId())
                                .file(file))
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidContentAndMessagePayloadMapping() throws Exception {

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
                processModelWithExtensions(project,
                                           "process-model",
                                           new Extensions(),
                                           resourceAsByteArray("process/message-payload-mapping.bpmn20.xml")));
        MockMultipartFile file = multipartExtensionsFile(
                processModel,
                resourceAsByteArray("process-extensions/valid-extensions-with-message-payload-mapping.json"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate/extensions",
                                  API_VERSION,
                                  processModel.getId())
                                .file(file))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessModelWithInvalidName() throws Exception {

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
                processModel(project,
                             "process-model"));
        MockMultipartFile file = multipartProcessFile(processModel,
                                                      resourceAsByteArray("process/invalid-process-name.bpmn20.xml"));

        final ResultActions resultActions = mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                                                      API_VERSION,
                                                                      processModel.getId())
                                                                    .file(file));

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .hasSize(2)
                .extracting(ModelValidationError::getDescription,
                            ModelValidationError::getValidatorSetName)
                .contains(tuple("The process name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'RankMovie'",
                                "DNS name validator"));
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidMappingContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-mapping-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());
        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
                .isEqualTo("#: #: only 1 subschema matches out of 2");

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .hasSize(2)
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsOnly(tuple("inputds is not a valid enum value",
                                    "#/extensions/mappings/ServiceTask_06crg3b/inputds: inputds is not a valid enum value"),
                              tuple("outputss is not a valid enum value",
                                    "#/extensions/mappings/ServiceTask_06crg3b/outputss: outputss is not a valid enum value"));
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidStringVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-string-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(tuple("expected type: String, found: Integer",
                                       "Mismatch value type - stringVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is string"));
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidIntegerVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-integer-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "Process-Model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(tuple("expected type: Number, found: String",
                                       "Mismatch value type - integerVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is integer"));
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidBooleanVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-boolean-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(tuple("expected type: Boolean, found: Integer",
                                       "Mismatch value type - booleanVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is boolean"));
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidObjectVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-object-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(tuple("expected type: JSONObject, found: Integer",
                                       "Mismatch value type - objectVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is json"));
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessExtensionsWithInvalidDateVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-date-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(
                        tuple("expected type: String, found: Integer",
                              "Mismatch value type - dateVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is date"),
                        tuple("string [aloha] does not match pattern ^[0-9]{4}-(((0[13578]|(10|12))-(0[1-9]|[1-2][0-9]|3[0-1]))|(02-(0[1-9]|[1-2][0-9]))|((0[469]|11)-(0[1-9]|[1-2][0-9]|30)))$",
                              "Invalid date - dateVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68)"));
    }

    @Test
    public void should_throwNotFoundException_when_validatingModelThatNotExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());
        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  "model_id")
                                .file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_throwNotFoundException_when_validatingInvalidProcessModelUsingTextContentType() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpmn20.xml",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  processModel.getId())
                                .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingConnectorValidContent() throws Exception {
        byte[] validContent = resourceAsByteArray("connector/connector-simple.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "connector-simple.json",
                                                       CONTENT_TYPE_JSON,
                                                       validContent);
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model connectorModel = modelRepository.createModel(connectorModel(project,
                                                                          "connector-model"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  connectorModel.getId())
                                .file(file))
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingConnectorValidContentWithTemplate() throws Exception {
        byte[] validContent = resourceAsByteArray("connector/connector-template.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "connector-template.json",
                                                       CONTENT_TYPE_JSON,
                                                       validContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model connectorModel = modelRepository.createModel(connectorModel(project,
                                                                          "connector-model"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  connectorModel.getId())
                                .file(file))
                .andExpect(status().isNoContent());
    }
    
    @Test
    public void should_throwExceptiojn_when_validatingProcessWithServiceTaskImplementationSetToUnknownConnectorAction() throws Exception {
        byte[] validContent = resourceAsByteArray("process/unknown-implementation-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "process.xml",
                                                       CONTENT_TYPE_XML,
                                                       validContent);
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   API_VERSION,
                                   processModel.getId())
                                 .file(file));

        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);
        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .hasSize(1)
                .extracting(ModelValidationError::getDescription,
                            ModelValidationError::getValidatorSetName)
                .contains(tuple("Invalid service implementation on service 'ServiceTask_1qr4ad0'","BPMN service task validator"));    
    }
    
    @Test
    public void should_returnStatusNoContent_when_validatingProcessWithServiceTaskImplementationSetToDMNAction() throws Exception {
        byte[] validContent = resourceAsByteArray("process/dmn-implementation-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "process.xml",
                                                       CONTENT_TYPE_XML,
                                                       validContent);
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   API_VERSION,
                                   processModel.getId())
                                 .file(file));

        resultActions.andExpect(status().isNoContent());
    }
    
    @Test
    public void should_returnStatusNoContent_when_validatingProcessWithServiceTaskImplementationSetToScriptAction() throws Exception {
        byte[] validContent = resourceAsByteArray("process/script-implementation-service-task.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "process.xml",
                                                       CONTENT_TYPE_XML,
                                                       validContent);
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   API_VERSION,
                                   processModel.getId())
                                 .file(file));

        resultActions.andExpect(status().isNoContent());
    }
    
    
    
    
}
