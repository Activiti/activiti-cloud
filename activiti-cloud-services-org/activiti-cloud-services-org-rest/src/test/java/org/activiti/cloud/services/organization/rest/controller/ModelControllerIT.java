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

package org.activiti.cloud.services.organization.rest.controller;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.api.process.Extensions;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.activiti.cloud.services.organization.entity.ProjectEntity;
import org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.mock.IsObjectEquals.isBooleanEquals;
import static org.activiti.cloud.services.organization.mock.IsObjectEquals.isDateEquals;
import static org.activiti.cloud.services.organization.mock.IsObjectEquals.isIntegerEquals;
import static org.activiti.cloud.services.organization.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.extensions;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithContent;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithExtensions;
import static org.activiti.cloud.services.organization.mock.MockFactory.project;
import static org.activiti.cloud.services.organization.mock.MockMultipartRequestBuilder.putMultipart;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.activiti.cloud.services.test.asserts.AssertResponseContent.assertThatResponseContent;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class ModelControllerIT {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ModelRepository modelRepository;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testGetModels() throws Exception {
        //given
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("Parent Project"));

        modelRepository.createModel(processModel(project,
                                                 "Process Model 1"));
        modelRepository.createModel(processModel(project,
                                                 "Process Model 2"));

        //when
        final ResultActions resultActions = mockMvc
                .perform(get("{version}/projects/{projectId}/models?type=PROCESS",
                             API_VERSION,
                             project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.models",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.models[0].name",
                                    is("Process Model 1")))
                .andExpect(jsonPath("$._embedded.models[1].name",
                                    is("Process Model 2")));
    }

    @Test
    public void testCreateModel() throws Exception {
        // GIVEN
        Project project = projectRepository.createProject(project("Parent Project"));

        // WHEN
        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel("Process Model"))))
                .andDo(print())
                // THEN
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.extensions.properties",
                                    notNullValue()))
                .andExpect(jsonPath("$.extensions.mappings",
                                    notNullValue()));

    }

    @Test
    public void testCreateProcessModelWithExtensions() throws Exception {
        // GIVEN
        Project project = projectRepository.createProject(project("Parent Project"));

        ModelEntity processModel = processModelWithExtensions("processModelWithExtensions",
                                                              extensions("variable1",
                                                                         "variable2"));
        // WHEN
        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel)))
                .andDo(print())
                // THEN
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.extensions.properties",
                                    allOf(hasKey("variable1"),
                                          hasKey("variable2"))))
                .andExpect(jsonPath("$.extensions.mappings",
                                    hasEntry(equalTo("ServiceTask"),
                                             allOf(hasEntry(equalTo("inputs"),
                                                            allOf(hasKey("variable1"),
                                                                  hasKey("variable2"))),
                                                   hasEntry(equalTo("outputs"),
                                                            allOf(hasKey("variable1"),
                                                                  hasKey("variable2")))
                                             ))
                ));
    }

    @Test
    public void testCreateModelOfUnknownType() throws Exception {
        // GIVEN
        Project project = projectRepository.createProject(project("Parent Project"));

        Model formModel = new ModelEntity("name",
                                          "FORM");

        // WHEN
        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(formModel)))
                .andDo(print())
                // THEN
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetModel() throws Exception {
        //given
        Model processModel = modelRepository.createModel(processModel("Process Model"));

        //then
        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            processModel.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetModelWithExtensions() throws Exception {
        //given
        Model processModel = modelRepository
                .createModel(processModelWithExtensions("processModelWithExtensions",
                                                        extensions("stringVariable",
                                                                   "integerVariable",
                                                                   "booleanVariable",
                                                                   "dateVariable",
                                                                   "jsonVariable")));
        //when
        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            processModel.getId()))
                .andDo(print())
                //then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extensions.properties",
                                    allOf(hasEntry(equalTo("stringVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("stringVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("stringVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("string")),
                                                         hasEntry(equalTo("value"),
                                                                  equalTo("stringVariable"))
                                                   )),
                                          hasEntry(equalTo("integerVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("integerVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("integerVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("integer")),
                                                         hasEntry(equalTo("value"),
                                                                  isIntegerEquals(15)))),
                                          hasEntry(equalTo("booleanVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("booleanVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("booleanVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("boolean")),
                                                         hasEntry(equalTo("value"),
                                                                  isBooleanEquals(true)))),
                                          hasEntry(equalTo("dateVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("dateVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("dateVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("date")),
                                                         hasEntry(equalTo("value"),
                                                                  isDateEquals(0)))),
                                          hasEntry(equalTo("jsonVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("jsonVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("jsonVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("json")),
                                                         hasEntry(equalTo("value"),
                                                                  isJson(withJsonPath("json-field-name")))))
                                    )))
                .andExpect(jsonPath("$.extensions.mappings",
                                    hasEntry(equalTo("ServiceTask"),
                                             allOf(hasEntry(equalTo("inputs"),
                                                            allOf(hasEntry(equalTo("stringVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("stringVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("integerVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("integerVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("booleanVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("booleanVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("dateVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("dateVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("jsonVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("jsonVariable"))
                                                                           )))),
                                                   hasEntry(equalTo("outputs"),
                                                            allOf(hasEntry(equalTo("stringVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("stringVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("integerVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("integerVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("booleanVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("booleanVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("dateVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("dateVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("jsonVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("jsonVariable"))
                                                                           ))))
                                             ))
                ));
    }

    @Test
    public void testCreateProcessModelInProject() throws Exception {
        //given
        Project parentProject = projectRepository.createProject(project("Parent Project"));

        //when
        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             parentProject.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel("Process Model"))))
                .andExpect(status().isCreated());
    }

    @Test
    public void testUpdateModel() throws Exception {
        //given
        Model processModel = modelRepository.createModel(processModel("Process Model"));

        //when
        mockMvc.perform(put("{version}/models/{modelId}",
                            API_VERSION,
                            processModel.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel("New Process Model"))))
                .andExpect(status().isOk());

        //then
        Optional<Model> optionalModel = modelRepository.findModelById(processModel.getId());
        assertThat(optionalModel).hasValueSatisfying(
                model -> assertThat(model.getName()).isEqualTo("New Process Model")
        );
    }

    @Test
    public void testUpdateModelWithExtensions() throws Exception {
        //given
        ModelEntity processModel = processModelWithExtensions("processModelWithExtensions",
                                                              extensions("variable1"));
        modelRepository.createModel(processModel);

        ModelEntity newModel = processModelWithExtensions("processModelWithExtensions",
                                                          extensions("variable2",
                                                                     "variable3"));
        //when
        mockMvc.perform(put("{version}/models/{modelId}",
                            API_VERSION,
                            processModel.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(newModel)))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteModel() throws Exception {
        //given
        Model processModel = modelRepository.createModel(processModel("Process Model"));

        //when
        mockMvc.perform(delete("{version}/models/{modelId}",
                               API_VERSION,
                               processModel.getId()))
                .andExpect(status().isNoContent());

        //then
        assertThat(modelRepository.findModelById(processModel.getId())).isEmpty();
    }

    @Test
    public void testGetModelTypes() throws Exception {

        mockMvc.perform(get("{version}/model-types",
                            API_VERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.model-types",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.model-types[0].name",
                                    is(PROCESS)))
                .andExpect(jsonPath("$._embedded.model-types[1].name",
                                    is(ConnectorModelType.NAME)));
    }

    @Test
    public void validateProcessModelWithValidContent() throws Exception {
        // given
        byte[] validContent = resourceAsByteArray("process/x-19022.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "process.xml",
                                                       CONTENT_TYPE_XML,
                                                       validContent);
        Model processModel = modelRepository.createModel(processModel("Process-Model"));

        // when
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId())
                                 .file(file))
                // then
                .andExpect(status().isNoContent());
    }

    @Test
    public void validateProcessModelWithInvalidContent() throws Exception {

        // given
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       CONTENT_TYPE_XML,
                                                       "BPMN diagram".getBytes());
        Model processModel = modelRepository.createModel(processModel("Process-Model"));

        // when
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId())
                                 .file(file))
                // then
                .andExpect(status().isBadRequest());
    }

    @Test
    public void validateProcessExtensionsWithValidContent() throws Exception {

        // given
        byte[] validContent = resourceAsByteArray("process-extensions/valid-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       validContent);

        Model processModel = modelRepository.createModel(processModelWithExtensions("Process-Model",
                                                                                    new Extensions()));

        // when
        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  RepositoryRestConfig.API_VERSION,
                                  processModel.getId())
                                .file(file))
                // then
                .andExpect(status().isNoContent());
    }

    @Test
    public void validateProcessExtensionsWithInvalidContent() throws Exception {

        // given
        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        Model processModel = modelRepository.createModel(processModelWithExtensions("Process-Model",
                                                                                    new Extensions()));
        // when
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        // then
        resultActions.andExpect(status().isBadRequest());
        assertThat(resultActions.andReturn().getResponse().getErrorMessage()).isEqualTo("#/extensions: 2 schema violations found");

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .hasSize(2)
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsOnly(tuple("inputds is not a valid enum value",
                                    "#/extensions/mappings/ServiceTask_06crg3b/inputds: inputds is not a valid enum value"),
                              tuple("required key [type] not found",
                                    "#/extensions/properties/db995012-b417-4cea-a981-cef287de8e4a: required key [type] not found"));
    }

    @Test
    public void validateModelThatNotExistsShouldThrowException() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());
        // when
        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  RepositoryRestConfig.API_VERSION,
                                  "model_id")
                                .file(file))
                // then
                .andExpect(status().isNotFound());
    }

    @Test
    public void validateInvalidProcessModelUsingTextContentType() throws Exception {

        // given
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpmn20.xml",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());
        Model processModel = modelRepository.createModel(processModel("Process-Model"));

        // when
        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  processModel.getId())
                                .file(file))
                // then
                .andExpect(status().isBadRequest());
    }

    @Test
    public void validateConnectorValidContent() throws Exception {
        // given
        byte[] validContent = resourceAsByteArray("connector/connector-simple.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "connector-simple.json",
                                                       CONTENT_TYPE_JSON,
                                                       validContent);
        Model connectorModel = modelRepository.createModel(connectorModel("Connector-Model"));

        // when
        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  connectorModel.getId())
                                .file(file))
                // then
                .andExpect(status().isNoContent());
    }

    @Test
    public void validateConnectorValidContentWithTemplate() throws Exception {
        // given
        byte[] validContent = resourceAsByteArray("connector/connector-template.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "connector-template.json",
                                                       CONTENT_TYPE_JSON,
                                                       validContent);
        Model connectorModel = modelRepository.createModel(connectorModel("Connector-Model"));

        // when
        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  connectorModel.getId())
                                .file(file))
                // then
                .andExpect(status().isNoContent());
    }

    @Test
    public void testExportModel() throws Exception {
        //GIVEN
        Model processModel = modelRepository.createModel(processModelWithContent("process_model_id",
                                                                                 "Process Model Content"));
        // WHEN
        MvcResult response = mockMvc.perform(
                get("{version}/models/{modelId}/export",
                    API_VERSION,
                    processModel.getId()))
                .andExpect(status().isOk())
                .andReturn();

        // THEN
        assertThatResponseContent(response)
                .isFile()
                .hasName("process_model_id.bpmn20.xml")
                .hasContentType(CONTENT_TYPE_XML)
                .hasContent("Process Model Content");
    }

    @Test
    public void testExportModelNotFound() throws Exception {
        // WHEN
        mockMvc.perform(
                get("{version}/models/not_existing_model/export",
                    API_VERSION))
                // THEN
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void testImportProcessModel() throws Exception {
        //GIVEN
        Project parentProject = projectRepository.createProject(project("Parent Project"));

        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022.bpmn20.xml",
                                                          "project/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        // WHEN
        mockMvc.perform(multipart("{version}/projects/{projectId}/models/import",
                                  API_VERSION,
                                  parentProject.getId())
                                .file(zipFile)
                                .param("type",
                                       PROCESS)
                                .accept(APPLICATION_JSON_VALUE))
                .andDo(print())
                // THEN
                .andExpect(status().isCreated());
    }

    @Test
    public void testImportModelWrongFileName() throws Exception {
        //GIVEN
        Project parentProject = project("Parent Project");
        projectRepository.createProject(parentProject);

        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022",
                                                          "project/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        // WHEN
        mockMvc.perform(multipart("{version}/projects/{projectId}/models/import",
                                  API_VERSION,
                                  parentProject.getId())
                                .file(zipFile)
                                .param("type",
                                       PROCESS)
                                .accept(APPLICATION_JSON_VALUE))
                // THEN
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("Unexpected extension was found for file to import model of type PROCESS: x-19022")));
    }

    @Test
    public void testImportModelWrongModelType() throws Exception {
        //GIVEN
        Project parentProject = project("Parent Project");
        projectRepository.createProject(parentProject);

        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022.xml",
                                                          "project/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        // WHEN
        mockMvc.perform(multipart("{version}/projects/{projectId}/models/import",
                                  API_VERSION,
                                  parentProject.getId())
                                .file(zipFile)
                                .param("type",
                                       "WRONG_TYPE"))
                // THEN
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("Unknown model type: WRONG_TYPE")));
    }

    @Test
    public void testImportModelProjectNotFound() throws Exception {
        //GIVEN
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022.xml",
                                                          "project/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        // WHEN
        mockMvc.perform(multipart("{version}/projects/not_existing_project/models/import",
                                  API_VERSION)
                                .file(zipFile)
                                .param("type",
                                       PROCESS))
                // THEN
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateConnectorTemplate() throws Exception {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("Connector With Template"));

        // WHEN
        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     connectorModel.getId())
                                .file("file",
                                      "connector-template.json",
                                      "application/json",
                                      resourceAsByteArray("connector/connector-template.json")))
                .andExpect(status().isNoContent());

        // THEN
        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            connectorModel.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template",
                                    is("ConnectorTemplate")));
    }

    @Test
    public void testUpdateConnectorCustom() throws Exception {
        //GIVEN
        Model connectorModel = modelRepository.createModel(connectorModel("SimpleConnector"));

        // WHEN
        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     connectorModel.getId())
                                .file("file",
                                      "connector-simple.json",
                                      "application/json",
                                      resourceAsByteArray("connector/connector-simple.json")))
                .andExpect(status().isNoContent());

        // THEN
        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            connectorModel.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template").doesNotExist());
    }
}
